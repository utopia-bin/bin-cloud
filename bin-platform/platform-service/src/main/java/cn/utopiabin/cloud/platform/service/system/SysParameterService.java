package cn.utopiabin.cloud.platform.service.system;

import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.system.SysParameter;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.system.SysParameterVO;
import cn.utopiabin.cloud.platform.repository.system.SysParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 系统参数服务
 * <p>
 * 提供系统参数的 CRUD 与缓存管理。
 * <p>
 * 缓存策略 (RedisClient 手动控制，支持主动预热与精准重建):
 * <ul>
 *   <li>缓存 Key: {@code SYS_PARAM:{key}}, Value: 参数值字符串, TTL: 7 天</li>
 *   <li>参数变更时精准重建对应 paramKey 的缓存 (查库 + 回填)</li>
 *   <li>{@link #refreshCache()} 全量清除并主动预热</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysParameterService {

    /** 参数缓存 Key 前缀 */
    private static final String CACHE_PREFIX = "SYS_PARAM:";

    /** 缓存 TTL (参数变更少，TTL 设为 7 天) */
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final SysParameterRepository parameterRepository;
    private final RedisClient redisClient;

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "参数管理", action = "新增参数", type = OperateType.CREATE, maskParams = true)
    public void create(SysParameterCreateDTO dto) {
        var key = dto.getParamKey().trim();
        if (parameterRepository.keyExists(key, null)) {
            throw new BizException(PlatformErrorCode.PARAM_KEY_DUPLICATE.getCode(),
                    PlatformErrorCode.PARAM_KEY_DUPLICATE.getMsg());
        }
        var entity = dto.copyTo(SysParameter.class);
        entity.setParamKey(key);
        entity.setParamValue(StrUtil.defaultIfBlank(dto.getParamValue(), ""));
        entity.setParamComment(StrUtil.defaultIfBlank(dto.getParamComment(), ""));
        entity.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        parameterRepository.save(entity);
        redisClient.set(cacheKey(entity.getParamKey()), entity.getParamValue(), CACHE_TTL);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "参数管理", action = "修改参数", type = OperateType.UPDATE, maskParams = true)
    public void update(SysParameterUpdateDTO dto) {
        var key = dto.getParamKey().trim();
        var entity = parameterRepository.getOrThrow(dto.getId());

        if (parameterRepository.keyExists(key, dto.getId())) {
            throw new BizException(PlatformErrorCode.PARAM_KEY_DUPLICATE.getCode(),
                    PlatformErrorCode.PARAM_KEY_DUPLICATE.getMsg());
        }

        var oldKey = entity.getParamKey();
        var keyChanged = !key.equals(oldKey);

        entity.setParamKey(key);
        entity.setParamValue(StrUtil.defaultIfBlank(dto.getParamValue(), ""));
        entity.setParamComment(StrUtil.defaultIfBlank(dto.getParamComment(), ""));
        entity.setSort(Optional.ofNullable(dto.getSort()).orElse(entity.getSort()));
        parameterRepository.updateById(entity);

        // key 变更时清除旧缓存
        if (keyChanged) {
            redisClient.delete(cacheKey(oldKey));
        }
        // 精准重建新缓存
        redisClient.set(cacheKey(entity.getParamKey()), entity.getParamValue(), CACHE_TTL);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "参数管理", action = "删除参数", type = OperateType.DELETE, maskParams = true)
    public void remove(Long id) {
        var entity = parameterRepository.getOrThrow(id);
        parameterRepository.removeById(id);
        redisClient.delete(cacheKey(entity.getParamKey()));
    }

    public PageResult<SysParameterVO> page(SysParameterPageQuery query) {
        var p = parameterRepository.page(query);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(e -> e.copyTo(SysParameterVO.class)).toList());
    }

    public List<SysParameterVO> list(SysParameterListQuery query) {
        return parameterRepository.list(query).stream()
                .map(e -> e.copyTo(SysParameterVO.class))
                .toList();
    }

    /**
     * 按参数键获取参数值（缓存优先）
     * <p>
     * 缓存命中时 0 次 DB 查询，未命中时 1 次查询并回填缓存。
     * 当数据库中不存在该 key 时不缓存，返回 defaultValue。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String getValue(String key, String defaultValue) {
        if (StrUtil.isBlank(key)) {
            return defaultValue;
        }
        var cached = redisClient.get(cacheKey(key), String.class);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        var entity = parameterRepository.findByKey(key);
        if (entity != null) {
            redisClient.set(cacheKey(key), entity.getParamValue(), CACHE_TTL);
            return entity.getParamValue();
        }
        return defaultValue;
    }

    /**
     * 刷新所有参数缓存
     * <p>
     * 清除所有参数缓存后主动预热：查出全部参数，逐个写入缓存。
     */
    @OperateLog(module = "参数管理", action = "刷新缓存", type = OperateType.UPDATE, maskParams = true)
    public void refreshCache() {
        var keys = redisClient.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisClient.delete(keys);
        }
        parameterRepository.list(new SysParameterListQuery())
                .forEach(e -> redisClient.set(cacheKey(e.getParamKey()), e.getParamValue(), CACHE_TTL));
        log.info("参数缓存刷新完成");
    }

    // ==================== 私有方法 ====================

    /**
     * 缓存 Key
     */
    private String cacheKey(String key) {
        return CACHE_PREFIX + key;
    }
}
