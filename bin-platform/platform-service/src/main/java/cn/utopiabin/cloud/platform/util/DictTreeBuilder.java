package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsItemVO;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsTreeVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典项树构建工具
 * <p>
 * 将扁平字典项列表转换为树形结构，消除 Service 层中的重复代码。
 * 与 {@link MenuTreeBuilder} 保持一致的架构风格。
 *
 * @since 1.0
 */
public final class DictTreeBuilder {

    /** 顶级字典项的 parentId */
    private static final Long ROOT_PARENT_ID = 0L;

    private DictTreeBuilder() {
    }

    /**
     * 构建字典项树
     *
     * @param items 扁平字典项列表
     * @return 树形结构列表
     */
    public static List<SysDictOptionsTreeVO> build(List<SysDictOptionsItemVO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<Long, List<SysDictOptionsTreeVO>> groupedByParent = items.stream()
                .map(DictTreeBuilder::toTreeVO)
                .collect(Collectors.groupingBy(SysDictOptionsTreeVO::getParentId));

        List<SysDictOptionsTreeVO> roots = groupedByParent.getOrDefault(ROOT_PARENT_ID, List.of());
        roots.forEach(root -> fillChildren(root, groupedByParent));

        return roots;
    }

    /**
     * 递归填充子节点
     */
    private static void fillChildren(SysDictOptionsTreeVO node, Map<Long, List<SysDictOptionsTreeVO>> groupedByParent) {
        List<SysDictOptionsTreeVO> children = groupedByParent.get(node.getId());
        if (children == null || children.isEmpty()) {
            return;
        }
        children.forEach(child -> fillChildren(child, groupedByParent));
        node.setChildren(children);
    }

    /**
     * 缓存项 → 树节点 VO 转换
     */
    private static SysDictOptionsTreeVO toTreeVO(SysDictOptionsItemVO source) {
        var vo = new SysDictOptionsTreeVO();
        vo.setId(source.getId());
        vo.setParentId(source.getParentId());
        vo.setName(source.getOptionName());
        vo.setValue(source.getOptionValue());
        return vo;
    }
}
