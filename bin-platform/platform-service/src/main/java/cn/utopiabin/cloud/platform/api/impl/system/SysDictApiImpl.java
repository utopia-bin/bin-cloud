package cn.utopiabin.cloud.platform.api.impl.system;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.api.system.SysDictApi;
import cn.utopiabin.cloud.platform.entity.system.SysDict;
import cn.utopiabin.cloud.platform.entity.system.SysDictOptions;
import cn.utopiabin.cloud.platform.model.dto.system.*;
import cn.utopiabin.cloud.platform.model.vo.system.*;
import cn.utopiabin.cloud.platform.repository.system.SysDictOptionsRepository;
import cn.utopiabin.cloud.platform.repository.system.SysDictRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统字典 API 实现（Dubbo 服务提供者，负责业务编排：校验 + 缓存 + VO 转换）
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class SysDictApiImpl implements SysDictApi {

    private static final String CACHE_PREFIX = "SYS_DICT:";
    private static final Duration CACHE_TTL = Duration.ofDays(30);
    private static final TypeReference<List<SysDictOptionsItemVO>> CACHE_TYPE = new TypeReference<>() {
    };

    private final SysDictRepository dictRepository;
    private final SysDictOptionsRepository optionsRepository;
    private final RedisClient redisClient;

    // ==================== 字典 CRUD ====================

    @Override
    public void saveDict(SysDictSaveDTO dto) {
        var name = dto.getName().trim();
        var code = dto.getCode().trim();
        var id = dto.getId();
        if (id == null) {
            if (dictRepository.countByNameOrCode(name, code, 0) > 0) {
                throw new BizException("字典名称或编码已存在");
            }
            var dict = dto.copyTo(SysDict.class);
            dict.setName(name);
            dict.setCode(code);
            dictRepository.save(dict);
        } else {
            var dict = dictRepository.getOrThrow(id);
            if (dictRepository.countByNameOrCode(name, code, id) > 0) {
                throw new BizException("字典名称或编码已存在");
            }
            var codeChanged = !code.equals(dict.getCode());
            dict.setName(name);
            dict.setCode(code);
            dict.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
            dict.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
            dict.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(false));
            dictRepository.updateById(dict);
            if (codeChanged) {
                refreshSingleCache(code, dict.getId());
            }
        }
    }

    @Override
    public void removeDict(Long id) {
        var dict = dictRepository.getOrThrow(id);
        optionsRepository.removeByDictId(id);
        dictRepository.removeById(id);
        redisClient.delete(cacheKey(dict.getCode()));
    }

    @Override
    public SysDictVO getDict(Long id) {
        return dictRepository.getOrThrow(id).copyTo(SysDictVO.class);
    }

    @Override
    public PageResult<SysDictVO> pageDict(SysDictPageQuery query) {
        var page = dictRepository.page(query);
        var records = page.getRecords().stream()
                .map(dict -> dict.copyTo(SysDictVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SysDictVO> listDict(SysDictListQuery query) {
        return dictRepository.list(query).stream()
                .map(dict -> dict.copyTo(SysDictVO.class))
                .toList();
    }

    // ==================== 字典项 CRUD ====================

    @Override
    public void saveDictOption(SysDictOptionsSaveDTO dto) {
        var dictId = dto.getDictId();
        var dict = dictRepository.getOrThrow(dictId);
        if (dto.getId() == null) {
            if (optionsRepository.countByNameOrValue(dictId, dto.getOptionName(), dto.getOptionValue(), 0) > 0) {
                throw new BizException("字典项名称或值已存在");
            }
            var option = dto.copyTo(SysDictOptions.class);
            option.setParentId(Optional.ofNullable(dto.getParentId()).orElse(0L));
            option.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
            optionsRepository.save(option);
        } else {
            if (dto.getId().equals(dto.getParentId())) {
                throw new BizException("上级字典项不能是自身");
            }
            var option = optionsRepository.getOrThrow(dto.getId());
            if (optionsRepository.countByNameOrValue(dictId, dto.getOptionName(), dto.getOptionValue(), dto.getId()) > 0) {
                throw new BizException("字典项名称或值已存在");
            }
            option.setParentId(Optional.ofNullable(dto.getParentId()).orElse(option.getParentId()));
            option.setOptionName(dto.getOptionName());
            option.setOptionValue(dto.getOptionValue());
            option.setOptionComment(StrUtil.defaultIfBlank(dto.getOptionComment(), ""));
            option.setSort(Optional.ofNullable(dto.getSort()).orElse(option.getSort()));
            optionsRepository.updateById(option);
        }
        refreshSingleCache(dict.getCode(), dictId);
    }

    @Override
    public void removeDictOption(Long id) {
        var option = optionsRepository.getOrThrow(id);
        if (optionsRepository.hasChild(id)) {
            throw new BizException("存在子级字典项，请先删除子级");
        }
        optionsRepository.removeById(id);
        refreshSingleCache(dictRepository.getCodeOrThrow(option.getDictId()), option.getDictId());
    }

    @Override
    public SysDictOptionsVO getDictOption(Long id) {
        return optionsRepository.getOrThrow(id).copyTo(SysDictOptionsVO.class);
    }

    @Override
    public PageResult<SysDictOptionsVO> pageDictOption(SysDictOptionsPageQuery query) {
        var page = optionsRepository.page(query);
        var records = page.getRecords().stream()
                .map(option -> option.copyTo(SysDictOptionsVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    // ==================== 缓存 ====================

    @Override
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
        return items;
    }

    @Override
    public void refreshDictCache() {
        redisClient.delete(redisClient.keys(CACHE_PREFIX + "*"));
        var all = optionsRepository.listWithCode(null);
        if (all == null || all.isEmpty()) {
            return;
        }
        all.stream().collect(Collectors.groupingBy(SysDictOptionsItemVO::getCode))
                .forEach((code, items) -> redisClient.set(cacheKey(code), JsonUtil.toJson(items), CACHE_TTL));
    }

    // ==================== 树形 ====================

    @Override
    public List<SysDictOptionsTreeVO> getDictTree(String dictCode) {
        var items = getDictItems(dictCode);
        return items == null || items.isEmpty() ? List.of() : buildTree(items);
    }

    @Override
    public List<SysDictOptionsTreeVO> getOptionalParents(Long dictId, Long excludeId) {
        var dict = dictRepository.getOrThrow(dictId);
        var items = getDictItems(dict.getCode());
        if (items == null) {
            return List.of();
        }
        var filtered = (excludeId != null && excludeId != 0)
                ? items.stream().filter(item -> !item.getId().equals(excludeId)).toList()
                : items;
        return filtered.stream().map(this::toDictNodeVO).toList();
    }

    @Override
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

    private void refreshSingleCache(String code, Long dictId) {
        var cacheVos = optionsRepository.listByDictId(dictId).stream().map(option -> {
            var vo = option.copyTo(SysDictOptionsItemVO.class);
            vo.setCode(code);
            return vo;
        }).toList();
        redisClient.set(cacheKey(code), JsonUtil.toJson(cacheVos), CACHE_TTL);
    }

    private List<SysDictOptionsTreeVO> buildTree(List<SysDictOptionsItemVO> items) {
        var groupedByParent = items.stream()
                .collect(Collectors.groupingBy(SysDictOptionsItemVO::getParentId));
        return groupedByParent.getOrDefault(0L, List.of()).stream()
                .map(root -> createTreeNode(root, 0L, groupedByParent))
                .toList();
    }

    private List<SysDictOptionsTreeVO> buildChildren(Long parentId, Map<Long, List<SysDictOptionsItemVO>> groupedByParent) {
        var children = groupedByParent.get(parentId);
        if (children == null) {
            return null;
        }
        return children.stream()
                .map(child -> createTreeNode(child, child.getParentId(), groupedByParent))
                .toList();
    }

    /**
     * 将缓存项转为树节点（复用逻辑，消除 buildTree / buildChildren / getOptionalParents 中的重复代码）
     */
    private SysDictOptionsTreeVO createTreeNode(SysDictOptionsItemVO source, Long parentId,
                                                Map<Long, List<SysDictOptionsItemVO>> groupedByParent) {
        var node = new SysDictOptionsTreeVO(source.getOptionName(), source.getOptionValue());
        node.setId(source.getId());
        node.setParentId(parentId);
        node.setChildren(buildChildren(source.getId(), groupedByParent));
        return node;
    }

    /**
     * 将缓存项转为扁平 VO（用于 getOptionalParents）
     */
    private SysDictOptionsTreeVO toDictNodeVO(SysDictOptionsItemVO item) {
        var vo = new SysDictOptionsTreeVO(item.getOptionName(), item.getOptionValue());
        vo.setId(item.getId());
        vo.setParentId(item.getParentId());
        return vo;
    }

    private String cacheKey(String code) {
        return CACHE_PREFIX + code;
    }
}
