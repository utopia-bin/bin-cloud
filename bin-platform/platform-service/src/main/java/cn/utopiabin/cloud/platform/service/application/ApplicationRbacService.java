package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.number;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireSingleChange;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireVersion;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.validateWindow;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationResourceDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationRoleDTO;
import cn.utopiabin.cloud.platform.model.dto.application.UserGrantDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationResourceVO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationRoleVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;
import cn.utopiabin.cloud.platform.repository.application.ApplicationRbacRepository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationRbacService {
    private final ApplicationRbacRepository repository;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    private void nonConsole(long app) {
        if (app == 1) throw new BizException(400, "平台壳的用户和角色请在原IAM页面维护");
    }

    @RequirePermission("platform:application:read")
    public List<UserApplicationVO> members(long instance) {
        var scope = boundary.instance(instance);
        long tenant = number(scope, "tenant_id");
        var rows = repository.listMembers(instance, tenant);
        rows.forEach(
                row ->
                        row.setRoleIds(
                                repository.listMemberRoleIds(tenant, instance, row.getUserId())));
        return rows;
    }

    @Transactional
    @RequirePermission("platform:application:grant")
    @OperateLog(module = "应用授权", action = "设置应用成员与角色", type = OperateType.UPDATE, maskParams = true)
    public void grant(UserGrantDTO dto) {
        var scope = boundary.instance(dto.getTenantApplicationId());
        long tenant = number(scope, "tenant_id"),
                app = number(scope, "application_id"),
                instance = dto.getTenantApplicationId();
        nonConsole(app);
        repository.lockInstance(instance, tenant);
        repository.requireUser(dto.getUserId(), tenant);
        validateWindow(dto.getEffectiveAt(), dto.getExpireAt());
        var roleIds = dto.getRoleIds().stream().distinct().toList();
        for (var id : roleIds) {
            repository.requireRole(id, tenant, app, instance);
        }
        var existing = repository.lockMember(tenant, instance, dto.getUserId());
        if (existing.isEmpty()) {
            if (dto.getExpectedVersion() != null) throw new BizException(409, "授权记录已变化，请刷新");
            repository.insertMember(tenant, instance, dto, boundary.userId());
        } else
            requireSingleChange(
                    repository.updateMember(
                            dto,
                            boundary.userId(),
                            existing.getFirst().get("id"),
                            requireVersion(dto.getExpectedVersion())));
        repository.replaceMemberRoles(tenant, app, instance, dto.getUserId(), roleIds);
        revocations.member(tenant, instance, dto.getUserId(), "MEMBERSHIP_CHANGED");
    }

    @RequirePermission("platform:application:read")
    public List<ApplicationRoleVO> roles(long instance) {
        var scope = boundary.instance(instance);
        long tenant = number(scope, "tenant_id");
        var rows = repository.listRoles(tenant, instance);
        rows.forEach(
                row ->
                        row.setPermissionIds(
                                repository.listRolePermissionIds(tenant, instance, row.getId())));
        return rows;
    }

    @Transactional
    @RequirePermission("platform:application:role")
    @OperateLog(module = "应用角色", action = "保存应用角色与权限", type = OperateType.UPDATE, maskParams = true)
    public long saveRole(ApplicationRoleDTO dto) {
        var scope = boundary.instance(dto.getTenantApplicationId());
        long tenant = number(scope, "tenant_id"),
                app = number(scope, "application_id"),
                instance = dto.getTenantApplicationId();
        nonConsole(app);
        repository.lockRoleInstance(instance, tenant);
        if (dto.getDataScope() != 1 && dto.getDataScope() != 4)
            throw new BizException(400, "仅支持全部或本人数据范围");
        var permissionIds = dto.getPermissionIds().stream().distinct().toList();
        for (var permission : permissionIds) {
            repository.requirePermission(permission, app);
        }
        long id = dto.getId() == null ? IdWorker.getId() : dto.getId();
        if (dto.getId() == null) repository.insertRole(id, tenant, app, instance, dto);
        else {
            var old = repository.getRole(id, tenant, app, instance);
            if ("app_admin".equals(old.get("code"))
                    && (!"app_admin".equals(dto.getCode()) || !dto.isAvailable()))
                throw new BizException(400, "内置应用管理员角色不能改编码或停用");
            requireSingleChange(
                    repository.updateRole(
                            id, tenant, instance, dto, requireVersion(dto.getExpectedVersion())));
        }
        repository.replaceRolePermissions(tenant, app, instance, id, permissionIds);
        revocations.instance(tenant, instance, "ROLE_CHANGED");
        return id;
    }

    @Transactional
    @RequirePermission("platform:application:role")
    @OperateLog(module = "应用角色", action = "删除应用角色", type = OperateType.DELETE, maskParams = true)
    public void removeRole(long instance, long id, int version) {
        var scope = boundary.instance(instance);
        long tenant = number(scope, "tenant_id");
        nonConsole(number(scope, "application_id"));
        var role = repository.lockRoleForDelete(id, tenant, instance);
        if ("app_admin".equals(role.get("code"))) throw new BizException(400, "不能删除内置应用管理员角色");
        requireSingleChange(repository.removeRole(id, tenant, instance, version));
        revocations.instance(tenant, instance, "ROLE_DELETED");
    }

    private boolean permissions(String kind) {
        if (!List.of("permissions", "menus").contains(kind)) {
            throw new BizException(400, "未知资源类型");
        }
        return "permissions".equals(kind);
    }

    @RequirePermission("platform:application:read")
    public List<ApplicationResourceVO> resources(long app, String kind) {
        return permissions(kind) ? repository.listPermissions(app) : repository.listMenus(app);
    }

    @Transactional
    @RequirePermission("platform:application:manage")
    @OperateLog(module = "应用资源", action = "保存应用权限或菜单", type = OperateType.UPDATE, maskParams = true)
    public long saveResource(String kind, ApplicationResourceDTO dto) {
        nonConsole(dto.getApplicationId());
        boolean permissionResource = permissions(kind);
        long app = dto.getApplicationId();
        repository.lockApplication(app);
        long id = dto.getId() == null ? IdWorker.getId() : dto.getId();
        Map<String, Object> old =
                dto.getId() == null
                        ? null
                        : permissionResource
                                ? repository.getPermission(id, app)
                                : repository.getMenu(id, app);
        if (permissionResource) {
            if (!dto.getCode().matches("[A-Za-z][A-Za-z0-9:_.*-]{0,99}"))
                throw new BizException(400, "请输入合法的应用权限编码，不允许全局通配符");
            if (old != null && !dto.getCode().equals(old.get("code")))
                throw new BizException(400, "已发布的权限编码不可修改，请新建权限并重新授权");
            if (old == null) repository.insertPermission(id, dto);
            else
                requireSingleChange(
                        repository.updatePermission(
                                id, dto, requireVersion(dto.getExpectedVersion())));
        } else {
            long parent = dto.getParentId() == null ? 0 : dto.getParentId();
            var visited = new HashSet<Long>();
            visited.add(id);
            for (long cursor = parent; cursor != 0; ) {
                if (!visited.add(cursor)) throw new BizException(400, "菜单父子关系不能形成循环");
                var node = repository.getMenuParent(cursor, app);
                if (number(node, "type") == 3) throw new BizException(400, "按钮不能包含子菜单");
                cursor = number(node, "parent_id");
            }
            if (!dto.getPermission().isBlank())
                repository.requirePermissionCode(dto.getPermission(), app);
            if (dto.getType() == 2 && !dto.getPath().matches("/[A-Za-z0-9_/-]+"))
                throw new BizException(400, "页面菜单必须填写应用内绝对路径");
            if (dto.getType() == 3 && repository.menuHasChildren(id, app))
                throw new BizException(400, "含下级的菜单不能改为按钮");
            if (old == null) repository.insertMenu(id, parent, dto);
            else
                requireSingleChange(
                        repository.updateMenu(
                                id, parent, dto, requireVersion(dto.getExpectedVersion())));
        }
        revocations.application(app, "RESOURCE_CHANGED");
        return id;
    }

    @Transactional
    @RequirePermission("platform:application:manage")
    @OperateLog(module = "应用资源", action = "删除应用资源", type = OperateType.DELETE, maskParams = true)
    public void removeResource(long app, String kind, long id, int version) {
        nonConsole(app);
        boolean permissionResource = permissions(kind);
        repository.lockApplicationForDelete(app);
        var old =
                permissionResource
                        ? repository.getPermission(id, app)
                        : repository.getMenu(id, app);
        if ("menus".equals(kind) && repository.hasChildMenu(app, id))
            throw new BizException(409, "请先删除下级菜单");
        if (permissionResource
                && repository.permissionIsReferenced(app, id, String.valueOf(old.get("code")))) {
            throw new BizException(409, "权限仍被角色或菜单引用，请先解除关联");
        }
        requireSingleChange(
                permissionResource
                        ? repository.removePermission(id, app, version)
                        : repository.removeMenu(id, app, version));
        revocations.application(app, "RESOURCE_DELETED");
    }
}
