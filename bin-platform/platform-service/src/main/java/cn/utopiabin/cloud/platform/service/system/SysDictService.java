package cn.utopiabin.cloud.platform.service.system;

import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.system.SysDict;
import cn.utopiabin.cloud.platform.entity.system.SysDictOptions;
import cn.utopiabin.cloud.platform.model.dto.system.*;
import cn.utopiabin.cloud.platform.model.vo.system.*;
import cn.utopiabin.cloud.platform.repository.system.SysDictOptionsRepository;
import cn.utopiabin.cloud.platform.repository.system.SysDictRepository;
import cn.utopiabin.cloud.platform.util.DictTreeBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统字典服务
 * <p>
 * 统一管理字典与字典项的业务逻辑：CRUD、缓存、树形构建。
 * <p>
 * 缓存策略 (RedisClient 手动控制，支持主动预热与精准重建):
 * <ul>
 *   <li>缓存 Key: {@code SYS_DICT:{code}}, Value: JSON, TTL: 30 天</li>
 *   <li>字典项变更时精准重建对应 dictCode 的缓存 (查库 + 回填)</li>
 *   <li>{@link #refreshDictCache()} 全量清除并主动预热</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictService {

    /** 字典项缓存 Key 前缀 */
    private static final String CACHE_PREFIX = "SYS_DICT:";

    /** 缓存 TTL (字典变更极少，TTL 设为 30 天) */
    private static final Duration CACHE_TTL = Duration.ofDays(30);

    /** 缓存反序列化 TypeReference */
    private static final TypeReference<List<SysDictOptionsItemVO>> CACHE_TYPE = new TypeReference<>() {
    };

    private final SysDictRepository dictRepository;
    private final SysDictOptionsRepository optionsRepository;
    private final RedisClient redisClient;

    // ==================== 字典 CRUD ====================

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "字典管理", action = "新增字典", type = OperateType.CREATE, maskParams = true)
    public void createDict(SysDictCreateDTO dto) {
        var name = dto.getName().trim();
        var code = dto.getCode().trim();
        if (dictRepository.countByNameOrCode(name, code, null) > 0) {
            throw new BizException(PlatformErrorCode.DICT_DUPLICATE.getCode(),
                    PlatformErrorCode.DICT_DUPLICATE.getMsg());
        }
        var dict = dto.copyTo(SysDict.class);
        dict.setName(name);
        dict.setCode(code);
        dict.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        dict.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        dict.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(false));
        dictRepository.save(dict);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "字典管理", action = "修改字典", type = OperateType.UPDATE, maskParams = true)
    public void updateDict(SysDictUpdateDTO dto) {
        var name = dto.getName().trim();
        var code = dto.getCode().trim();
        var dict = dictRepository.getOrThrow(dto.getId());

        if (dictRepository.countByNameOrCode(name, code, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.DICT_DUPLICATE.getCode(),
                    PlatformErrorCode.DICT_DUPLICATE.getMsg());
        }

        var oldCode = dict.getCode();
        var codeChanged = !code.equals(oldCode);

        dict.setName(name);
        dict.setCode(code);
        dict.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        dict.setSort(Optional.ofNullable(dto.getSort()).orElse(dict.getSort()));
        dict.setAvailable(dto.getAvailable());
        dictRepository.updateById(dict);

        // 编码变更时清除旧缓存（新缓存懒加载）
        if (codeChanged) {
            redisClient.delete(cacheKey(oldCode));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "字典管理", action = "删除字典", type = OperateType.DELETE, maskParams = true)
    public void removeDict(Long id) {
        var dict = dictRepository.getOrThrow(id);
        optionsRepository.removeByDictId(id);
        dictRepository.removeById(id);
        redisClient.delete(cacheKey(dict.getCode()));
    }

    public SysDictVO getDict(Long id) {
        return dictRepository.getOrThrow(id).copyTo(SysDictVO.class);
    }

    public PageResult<SysDictVO> pageDict(SysDictPageQuery query) {
        var page = dictRepository.page(query);
        var records = page.getRecords().stream()
                .map(dict -> dict.copyTo(SysDictVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public List<SysDictVO> listDict(SysDictListQuery query) {
        return dictRepository.list(query).stream()
                .map(dict -> dict.copyTo(SysDictVO.class))
                .toList();
    }

    // ==================== 字典项 CRUD ====================

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "字典管理", action = "新增字典项", type = OperateType.CREATE, maskParams = true)
    public void createDictOption(SysDictOptionsCreateDTO dto) {
        var dictId = dto.getDictId();
        dictRepository.getOrThrow(dictId);

        var name = dto.getOptionName().trim();
        var value = dto.getOptionValue().trim();
        if (optionsRepository.countByNameOrValue(dictId, name, value, null) > 0) {
            throw new BizException(PlatformErrorCode.DICT_OPTION_DUPLICATE.getCode(),
                    PlatformErrorCode.DICT_OPTION_DUPLICATE.getMsg());
        }

        var option = dto.copyTo(SysDictOptions.class);
        option.setParentId(Optional.ofNullable(dto.getParentId()).orElse(0L));
        option.setOptionName(name);
        option.setOptionValue(value);
        option.setOptionComment(StrUtil.defaultIfBlank(dto.getOptionComment(), ""));
        option.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        optionsRepository.save(option);

        rebuildDictCacheByDictId(dictId);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "字典管理", action = "修改字典项", type = OperateType.UPDATE, maskParams = true)
    public void updateDictOption(SysDictOptionsUpdateDTO dto) {
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(PlatformErrorCode.DICT_OPTION_PARENT_SELF.getCode(),
                    PlatformErrorCode.DICT_OPTION_PARENT_SELF.getMsg());
        }

        var option = optionsRepository.getOrThrow(dto.getId());
        var dictId = dto.getDictId();
        dictRepository.getOrThrow(dictId);

        var name = dto.getOptionName().trim();
        var value = dto.getOptionValue().trim();
        if (optionsRepository.countByNameOrValue(dictId, name, value, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.DICT_OPTION_DUPLICATE.getCode(),
                    PlatformErrorCode.DICT_OPTION_DUPLICATE.getMsg());
        }

        option.setDictId(dictId);
        option.setParentId(Optional.ofNullable(dto.getParentId()).orElse(option.getParentId()));
        option.setOptionName(name);
        option.setOptionValue(value);
        option.setOptionComment(StrUtil.defaultIfBlank(dto.getOptionComment(), ""));
        option.setSort(Optional.ofNullable(dto.getSort()).orElse(option.getSort()));
        optionsRepository.updateById(option);

        rebuildDictCacheByDictId(dictId);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperateLog(module = "字典管理", action = "删除字典项", type = OperateType.DELETE, maskParams = true)
    public void removeDictOption(Long id) {
        var option = optionsRepository.getOrThrow(id);
        if (optionsRepository.hasChild(id)) {
            throw new BizException(PlatformErrorCode.DICT_OPTION_HAS_CHILDREN.getCode(),
                    PlatformErrorCode.DICT_OPTION_HAS_CHILDREN.getMsg());
        }
        optionsRepository.removeById(id);
        rebuildDictCacheByDictId(option.getDictId());
    }

    public SysDictOptionsVO getDictOption(Long id) {
        return optionsRepository.getOrThrow(id).copyTo(SysDictOptionsVO.class);
    }

    public PageResult<SysDictOptionsVO> pageDictOption(SysDictOptionsPageQuery query) {
        var page = optionsRepository.page(query);
        var records = page.getRecords().stream()
                .map(option -> option.copyTo(SysDictOptionsVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    // ==================== 缓存 ====================

    /**
     * 获取字典项列表（缓存优先）
     * <p>
     * 缓存命中时 0 次 DB 查询，未命中时 1 次 JOIN 查询并回填缓存。
     *
     * @param dictCode 字典编码
     * @return 字典项精简 VO 列表
     */
    public List<SysDictOptionsItemVO> getDictItems(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            return List.of();
        }
        String cached = redisClient.get(cacheKey(dictCode), String.class);
        if (StrUtil.isNotBlank(cached)) {
            return JsonUtil.toObject(cached, CACHE_TYPE);
        }
        var items = optionsRepository.listWithCode(dictCode);
        if (items != null && !items.isEmpty()) {
            redisClient.set(cacheKey(dictCode), JsonUtil.toJson(items), CACHE_TTL);
        }
        return items != null ? items : List.of();
    }

    /**
     * 刷新全部字典缓存
     * <p>
     * 清除所有字典缓存后主动预热：查出全部字典项，按编码分组后逐个写入缓存。
     */
    @OperateLog(module = "字典管理", action = "刷新缓存", type = OperateType.UPDATE, maskParams = true)
    public void refreshDictCache() {
        var keys = redisClient.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisClient.delete(keys);
        }
        var all = optionsRepository.listWithCode(null);
        if (all == null || all.isEmpty()) {
            return;
        }
        all.stream().collect(Collectors.groupingBy(SysDictOptionsItemVO::getCode))
                .forEach((code, items) -> redisClient.set(cacheKey(code), JsonUtil.toJson(items), CACHE_TTL));
        log.info("字典缓存刷新完成: {} 个字典", all.stream().map(SysDictOptionsItemVO::getCode).distinct().count());
    }

    // ==================== 树形 ====================

    public List<SysDictOptionsTreeVO> getDictTree(String dictCode) {
        var items = getDictItems(dictCode);
        return DictTreeBuilder.build(items);
    }

    public List<SysDictOptionsTreeVO> getOptionalParents(Long dictId, Long excludeId) {
        dictRepository.getOrThrow(dictId);
        var code = dictRepository.getCodeOrThrow(dictId);
        var items = getDictItems(code);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        var filtered = (excludeId != null && excludeId != 0)
                ? items.stream().filter(item -> !item.getId().equals(excludeId)).toList()
                : items;
        return filtered.stream().map(this::toFlatNode).toList();
    }

    public List<SysDictOptionsMulTreeVO> getMulDictTree(String codes) {
        if (StrUtil.isBlank(codes)) {
            return List.of();
        }
        return Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> new SysDictOptionsMulTreeVO(code, getDictTree(code)))
                .toList();
    }

    // ==================== 私有方法 ====================

    /**
     * 精准重建指定 dictCode 的字典项缓存
     * <p>
     * 查出该字典的所有字典项，转换为缓存 VO 后写入 Redis。
     */
    private void rebuildDictCache(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            return;
        }
        var items = optionsRepository.listWithCode(dictCode);
        redisClient.set(cacheKey(dictCode), JsonUtil.toJson(items), CACHE_TTL);
    }

    /**
     * 通过字典 ID 查出编码后重建缓存
     */
    private void rebuildDictCacheByDictId(Long dictId) {
        var code = dictRepository.getCodeOrThrow(dictId);
        if (code != null) {
            rebuildDictCache(code);
        }
    }

    /**
     * 缓存 Key
     */
    private String cacheKey(String code) {
        return CACHE_PREFIX + code;
    }

    /**
     * 缓存项 → 扁平树节点 VO（用于 getOptionalParents）
     */
    private SysDictOptionsTreeVO toFlatNode(SysDictOptionsItemVO item) {
        var vo = new SysDictOptionsTreeVO();
        vo.setId(item.getId());
        vo.setParentId(item.getParentId());
        vo.setName(item.getOptionName());
        vo.setValue(item.getOptionValue());
        return vo;
    }
}
