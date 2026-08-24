package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.platform.constant.CacheConstants;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper;
import cn.utopiabin.cloud.platform.mapper.iam.SysUserMapper;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.UserPermissionVO;
import cn.utopiabin.cloud.platform.util.MenuTreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限聚合服务
 * <p>
 * 统一管理用户权限数据的查询与缓存。通过 JOIN 查询优化 N+1 问题，
 * 通过 Spring Cache 缓存用户权限聚合结果，减少登录/currentUser 的 DB 访问。
 * <p>
 * 缓存策略:
 * <ul>
 *   <li>缓存名: {@link CacheConstants#USER_PERM}, Key: userId, TTL: 30 分钟</li>
 *   <li>角色/菜单变更时调用 {@link #evictAllUserPermissions()} 全量失效</li>
 *   <li>用户角色变更时调用 {@link #evictUserPermission(Long)} 精准失效</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;

    /**
     * 获取用户权限聚合 (含角色列表、菜单列表、菜单树)
     * <p>
     * 缓存命中时 0 次 DB 查询，未命中时 2 次 JOIN 查询。
     *
     * @param userId 用户 ID
     * @return 用户权限聚合
     */
    @Cacheable(value = CacheConstants.USER_PERM, key = "#userId")
    public UserPermissionVO getUserPermissions(Long userId) {
        // 1. 查询用户角色 (JOIN sys_user_role + sys_role, 单次查询)
        List<SysRole> roles = userMapper.selectRolesByUserId(userId);
        var roleIds = roles.stream().map(SysRole::getId).toList();
        var roleVOs = roles.stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();

        // 2. 查询角色拥有的权限资源。菜单只是权限的导航投影，不参与授权决策。
        List<SysPermission> permissions = roleIds.isEmpty()
                ? List.of()
                : permissionMapper.selectByRoleIds(roleIds);
        var permissionCodes = permissions.stream()
                .map(SysPermission::getCode)
                .distinct()
                .toList();
        boolean allPermissions = permissionCodes.contains("*");

        // 3. 根据有效权限投影当前用户菜单。
        List<SysMenu> menus = menuMapper.selectMenusByPermissionCodes(permissionCodes, allPermissions);
        var menuIds = menus.stream().map(SysMenu::getId).toList();
        var menuVOs = menus.stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();

        // 4. 构建菜单树
        List<SysMenuTreeVO> menuTree = MenuTreeBuilder.build(menus);

        log.debug("加载用户权限: userId={}, roles={}, menus={}", userId, roles.size(), menus.size());
        return new UserPermissionVO(roleIds, roleVOs, permissionCodes, menuIds, menuVOs, menuTree);
    }

    /** 服务端权限判定，禁止以菜单是否可见代替授权。 */
    public boolean hasPermission(Long userId, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        var codes = getUserPermissions(userId).getPermissionCodes();
        return codes.contains("*") || codes.contains(permissionCode);
    }

    /**
     * 失效指定用户的权限缓存
     *
     * @param userId 用户 ID
     */
    @CacheEvict(value = CacheConstants.USER_PERM, key = "#userId")
    public void evictUserPermission(Long userId) {
        log.debug("失效用户权限缓存: userId={}", userId);
    }

    /**
     * 失效所有用户的权限缓存
     * <p>
     * 在角色/菜单变更时调用，因为可能影响多个用户。
     */
    @CacheEvict(value = CacheConstants.USER_PERM, allEntries = true)
    public void evictAllUserPermissions() {
        log.debug("失效所有用户权限缓存");
    }
}
