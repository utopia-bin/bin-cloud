package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysRoleMenu;
import cn.utopiabin.cloud.platform.mapper.iam.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 角色菜单关联 Repository
 *
 * @since 1.0
 */
@Repository
public class SysRoleMenuRepository extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> {

    /**
     * 为角色分配菜单 (全量替换)
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        removeByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        var list = menuIds.stream().map(menuId -> {
            var rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            return rm;
        }).toList();
        saveBatch(list);
    }

    /**
     * 获取角色拥有的菜单 ID 列表
     */
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        var list = list(new LambdaQueryWrapper<SysRoleMenu>()
                .select(SysRoleMenu::getMenuId)
                .eq(SysRoleMenu::getRoleId, roleId));
        return list == null || list.isEmpty() ? Collections.emptyList()
                : list.stream().map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 根据角色 ID 列表获取菜单 ID 列表
     */
    public List<Long> getMenuIdsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        var list = list(new LambdaQueryWrapper<SysRoleMenu>()
                .select(SysRoleMenu::getMenuId)
                .in(SysRoleMenu::getRoleId, roleIds));
        return list == null || list.isEmpty() ? Collections.emptyList()
                : list.stream().map(SysRoleMenu::getMenuId).distinct().toList();
    }

    /**
     * 删除角色的所有菜单关联
     */
    public void removeByRoleId(Long roleId) {
        remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
    }

    /**
     * 删除菜单的所有角色关联
     */
    public void removeByMenuId(Long menuId) {
        remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, menuId));
    }
}
