package cn.utopiabin.cloud.platform.api.impl.system;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.api.system.SysParameterApi;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterSaveDTO;
import cn.utopiabin.cloud.platform.entity.system.SysParameter;
import cn.utopiabin.cloud.platform.repository.system.SysParameterRepository;
import cn.utopiabin.cloud.platform.model.vo.system.SysParameterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 系统参数 API 实现（Dubbo 服务提供者，负责业务编排：校验 + 缓存 + VO 转换）
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class SysParameterApiImpl implements SysParameterApi {

    private static final String CACHE_PREFIX = "SYS_PARAM:";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final SysParameterRepository parameterRepository;
    private final RedisClient redisClient;

    @Override
    public void save(SysParameterSaveDTO dto) {
        var id = dto.getId();
        if (id == null) {
            if (parameterRepository.keyExists(dto.getParamKey(), 0)) {
                throw new BizException("参数Key已存在");
            }
            var e = dto.copyTo(SysParameter.class);
            e.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
            parameterRepository.save(e);
            redisClient.set(cacheKey(e.getParamKey()), e.getParamValue(), CACHE_TTL);
        } else {
            if (parameterRepository.keyExists(dto.getParamKey(), id)) {
                throw new BizException("参数Key已存在");
            }
            var e = parameterRepository.getOrThrow(id);
            e.setParamKey(dto.getParamKey());
            e.setParamValue(dto.getParamValue());
            e.setParamComment(StrUtil.defaultIfBlank(dto.getParamComment(), ""));
            e.setSort(Optional.ofNullable(dto.getSort()).orElse(e.getSort()));
            parameterRepository.updateById(e);
            redisClient.set(cacheKey(e.getParamKey()), e.getParamValue(), CACHE_TTL);
        }
    }

    @Override
    public void remove(Long id) {
        var e = parameterRepository.getOrThrow(id);
        parameterRepository.removeById(id);
        redisClient.delete(cacheKey(e.getParamKey()));
    }

    @Override
    public PageResult<SysParameterVO> page(SysParameterPageQuery query) {
        var p = parameterRepository.page(query);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(e -> e.copyTo(SysParameterVO.class)).toList());
    }

    @Override
    public List<SysParameterVO> list(SysParameterListQuery query) {
        return parameterRepository.list(query).stream()
                .map(e -> e.copyTo(SysParameterVO.class)).toList();
    }

    @Override
    public String getValue(String key, String defaultValue) {
        var cache = redisClient.get(cacheKey(key), String.class);
        if (StrUtil.isNotBlank(cache)) {
            return cache;
        }
        var e = parameterRepository.findByKey(key);
        if (e != null) {
            redisClient.set(cacheKey(key), e.getParamValue(), CACHE_TTL);
            return e.getParamValue();
        }
        return defaultValue;
    }

    @Override
    public void refreshCache() {
        if (!redisClient.keys(CACHE_PREFIX + "*").isEmpty()) {
            redisClient.delete(redisClient.keys(CACHE_PREFIX + "*"));
        }
        parameterRepository.list(new SysParameterListQuery())
                .forEach(e -> redisClient.set(cacheKey(e.getParamKey()), e.getParamValue(), CACHE_TTL));
    }

    private String cacheKey(String key) {
        return CACHE_PREFIX + key;
    }
}
