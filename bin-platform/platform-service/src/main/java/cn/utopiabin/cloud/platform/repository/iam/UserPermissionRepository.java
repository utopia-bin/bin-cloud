package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.UserPermissionVO;
import cn.utopiabin.cloud.platform.util.MenuTreeBuilder;
import java.util.List;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/** 平台 IAM 用户权限及导航投影查询仓库。 */
@Repository
@RequiredArgsConstructor
public class UserPermissionRepository {
    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysMenuRepository menuRepository;

    public UserPermissionVO getUserPermissions(Long userId) {
        // 1. 查询用户角色 (JOIN sys_user_role + sys_role, 单次查询)
        List<SysRole> roles = roleRepository.listByUserId(userId);
        var roleIds = roles.stream().map(SysRole::getId).toList();
        var roleVOs = roles.stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();

        // 2. 查询角色拥有的权限资源。菜单只是权限的导航投影，不参与授权决策。
        List<SysPermission> permissions = roleIds.isEmpty()
                ? List.of()
                : permissionRepository.listByRoleIds(roleIds);
        var permissionCodes = permissions.stream()
                .map(SysPermission::getCode)
                .distinct()
                .toList();

        // 3. 根据有效权限投影当前用户菜单。
        List<SysMenu> menus = menuRepository.listByPermissionCodes(permissionCodes);
        var menuIds = menus.stream().map(SysMenu::getId).toList();
        var menuVOs = menus.stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();

        // 4. 构建菜单树
        List<SysMenuTreeVO> menuTree = MenuTreeBuilder.build(menus);

        return new UserPermissionVO(roleIds, roleVOs, permissionCodes, menuIds, menuVOs, menuTree);
    }
}
