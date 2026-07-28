package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单树构建工具
 * <p>
 * 将扁平菜单列表转换为树形结构，消除 AuthApiImpl / SysMenuApiImpl 中的重复代码。
 *
 * @since 1.0
 */
public final class MenuTreeBuilder {

    /** 顶级菜单的 parentId */
    private static final Long ROOT_PARENT_ID = 0L;

    private MenuTreeBuilder() {
    }

    /**
     * 构建菜单树
     *
     * @param menus 扁平菜单列表
     * @return 树形结构列表
     */
    public static List<SysMenuTreeVO> build(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        // 转换 VO 并按 parentId 分组
        Map<Long, List<SysMenuTreeVO>> groupedByParent = menus.stream()
                .map(MenuTreeBuilder::toTreeVO)
                .collect(Collectors.groupingBy(SysMenuTreeVO::getParentId));

        // 从根节点开始递归构建子树
        List<SysMenuTreeVO> roots = groupedByParent.getOrDefault(ROOT_PARENT_ID, List.of());
        roots.forEach(root -> fillChildren(root, groupedByParent));

        // 根节点按 sort 排序
        roots.sort(Comparator.comparing(SysMenuTreeVO::getSort,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return roots;
    }

    /**
     * 递归填充子节点
     */
    private static void fillChildren(SysMenuTreeVO node, Map<Long, List<SysMenuTreeVO>> groupedByParent) {
        List<SysMenuTreeVO> children = groupedByParent.get(node.getId());
        if (children == null || children.isEmpty()) {
            return;
        }
        children.sort(Comparator.comparing(SysMenuTreeVO::getSort,
                Comparator.nullsLast(Comparator.naturalOrder())));
        children.forEach(child -> fillChildren(child, groupedByParent));
        node.setChildren(children);
    }

    /**
     * Entity → TreeVO 转换
     */
    private static SysMenuTreeVO toTreeVO(SysMenu menu) {
        var vo = new SysMenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setType(menu.getType());
        vo.setName(menu.getName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setPermission(menu.getPermission());
        vo.setSort(menu.getSort());
        vo.setVisible(menu.getVisible());
        vo.setAvailable(menu.getAvailable());
        return vo;
    }
}
