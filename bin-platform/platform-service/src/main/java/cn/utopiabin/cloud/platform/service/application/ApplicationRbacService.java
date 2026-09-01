package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.changed;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.number;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.version;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.window;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationResourceDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationRoleDTO;
import cn.utopiabin.cloud.platform.model.dto.application.UserGrantDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationResourceVO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationRoleVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;

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
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    private void nonConsole(long app) {
        if (app == 1) throw new BizException(400, "平台壳的用户和角色请在原IAM页面维护");
    }

    public List<UserApplicationVO> members(long instance) {
        var scope = boundary.instance(instance, "read");
        long tenant = number(scope, "tenant_id");
        var rows =
                store.list(
                        UserApplicationVO.class,
                        "applicationRbacServiceSelect15",
                        instance,
                        instance,
                        tenant);
        rows.forEach(
                row ->
                        row.setRoleIds(
                                store.queryForList(
                                        "applicationRbacServiceSelect18",
                                        Long.class,
                                        tenant,
                                        instance,
                                        row.getUserId())));
        return rows;
    }

    @Transactional
    @OperateLog(module = "应用授权", action = "设置应用成员与角色", type = OperateType.UPDATE, maskParams = true)
    public void grant(UserGrantDTO dto) {
        var scope = boundary.instance(dto.getTenantApplicationId(), "grant");
        long tenant = number(scope, "tenant_id"),
                app = number(scope, "application_id"),
                instance = dto.getTenantApplicationId();
        nonConsole(app);
        store.one("applicationRbacServiceSelect01", instance, tenant);
        store.one("applicationRbacServiceSelect02", dto.getUserId(), tenant);
        window(dto.getEffectiveAt(), dto.getExpireAt());
        var roleIds = dto.getRoleIds().stream().distinct().toList();
        for (var id : roleIds)
            store.one("applicationRbacServiceSelect03", id, tenant, app, instance);
        var existing =
                store.queryForList(
                        "applicationRbacServiceSelect19", tenant, instance, dto.getUserId());
        if (existing.isEmpty()) {
            if (dto.getExpectedVersion() != null) throw new BizException(409, "授权记录已变化，请刷新");
            store.update(
                    "applicationRbacServiceUpdate25",
                    IdWorker.getId(),
                    tenant,
                    instance,
                    dto.getUserId(),
                    dto.getStatus(),
                    dto.getEffectiveAt(),
                    dto.getExpireAt(),
                    boundary.userId(),
                    dto.getComment());
        } else
            changed(
                    store.update(
                            "applicationRbacServiceUpdate26",
                            dto.getStatus(),
                            dto.getEffectiveAt(),
                            dto.getExpireAt(),
                            dto.getComment(),
                            boundary.userId(),
                            existing.getFirst().get("id"),
                            version(dto.getExpectedVersion())));
        store.update("applicationRbacServiceUpdate27", tenant, instance, dto.getUserId());
        for (var id : roleIds)
            store.update(
                    "applicationRbacServiceUpdate28",
                    IdWorker.getId(),
                    tenant,
                    app,
                    instance,
                    dto.getUserId(),
                    id);
        revocations.member(tenant, instance, dto.getUserId(), "MEMBERSHIP_CHANGED");
    }

    public List<ApplicationRoleVO> roles(long instance) {
        var scope = boundary.instance(instance, "read");
        long tenant = number(scope, "tenant_id");
        var rows =
                store.list(
                        ApplicationRoleVO.class,
                        "applicationRbacServiceSelect16",
                        tenant,
                        instance);
        rows.forEach(
                row ->
                        row.setPermissionIds(
                                store.queryForList(
                                        "applicationRbacServiceSelect20",
                                        Long.class,
                                        tenant,
                                        instance,
                                        row.getId())));
        return rows;
    }

    @Transactional
    @OperateLog(module = "应用角色", action = "保存应用角色与权限", type = OperateType.UPDATE, maskParams = true)
    public long saveRole(ApplicationRoleDTO dto) {
        var scope = boundary.instance(dto.getTenantApplicationId(), "role");
        long tenant = number(scope, "tenant_id"),
                app = number(scope, "application_id"),
                instance = dto.getTenantApplicationId();
        nonConsole(app);
        store.one("applicationRbacServiceSelect04", instance, tenant);
        if (dto.getDataScope() != 1 && dto.getDataScope() != 4)
            throw new BizException(400, "仅支持全部或本人数据范围");
        for (var permission : dto.getPermissionIds().stream().distinct().toList())
            store.one("applicationRbacServiceSelect05", permission, app);
        long id = dto.getId() == null ? IdWorker.getId() : dto.getId();
        if (dto.getId() == null)
            store.update(
                    "applicationRbacServiceUpdate29",
                    id,
                    tenant,
                    app,
                    instance,
                    dto.getName(),
                    dto.getCode(),
                    dto.getDataScope(),
                    dto.isAvailable(),
                    dto.getSort());
        else {
            var old = store.one("applicationRbacServiceSelect06", id, tenant, app, instance);
            if ("app_admin".equals(old.get("code"))
                    && (!"app_admin".equals(dto.getCode()) || !dto.isAvailable()))
                throw new BizException(400, "内置应用管理员角色不能改编码或停用");
            changed(
                    store.update(
                            "applicationRbacServiceUpdate30",
                            dto.getName(),
                            dto.getCode(),
                            dto.getDataScope(),
                            dto.isAvailable(),
                            dto.getSort(),
                            id,
                            tenant,
                            instance,
                            version(dto.getExpectedVersion())));
        }
        store.update("applicationRbacServiceUpdate31", tenant, instance, id);
        for (var permission : dto.getPermissionIds().stream().distinct().toList())
            store.update(
                    "applicationRbacServiceUpdate32",
                    IdWorker.getId(),
                    tenant,
                    app,
                    instance,
                    id,
                    permission);
        revocations.instance(tenant, instance, "ROLE_CHANGED");
        return id;
    }

    @Transactional
    @OperateLog(module = "应用角色", action = "删除应用角色", type = OperateType.DELETE, maskParams = true)
    public void removeRole(long instance, long id, int version) {
        var scope = boundary.instance(instance, "role");
        long tenant = number(scope, "tenant_id");
        nonConsole(number(scope, "application_id"));
        store.one("applicationRbacServiceSelect07", instance, tenant);
        var role = store.one("applicationRbacServiceSelect08", id, tenant, instance);
        if ("app_admin".equals(role.get("code"))) throw new BizException(400, "不能删除内置应用管理员角色");
        changed(store.update("applicationRbacServiceUpdate33", id, tenant, instance, version));
        store.update("applicationRbacServiceUpdate34", tenant, instance, id);
        store.update("applicationRbacServiceUpdate35", tenant, instance, id);
        revocations.instance(tenant, instance, "ROLE_DELETED");
    }

    private boolean permissions(String kind) {
        if (!List.of("permissions", "menus").contains(kind)) {
            throw new BizException(400, "未知资源类型");
        }
        return "permissions".equals(kind);
    }

    public List<ApplicationResourceVO> resources(long app, String kind) {
        boundary.require("read");
        return store.list(
                ApplicationResourceVO.class,
                permissions(kind) ? "applicationPermissions" : "applicationMenus",
                app);
    }

    @Transactional
    @OperateLog(module = "应用资源", action = "保存应用权限或菜单", type = OperateType.UPDATE, maskParams = true)
    public long saveResource(String kind, ApplicationResourceDTO dto) {
        boundary.require("manage");
        nonConsole(dto.getApplicationId());
        boolean permissionResource = permissions(kind);
        long app = dto.getApplicationId();
        store.one("applicationRbacServiceSelect09", app);
        long id = dto.getId() == null ? IdWorker.getId() : dto.getId();
        Map<String, Object> old =
                dto.getId() == null
                        ? null
                        : store.one(
                                permissionResource ? "applicationPermission" : "applicationMenu",
                                id,
                                app);
        if (permissionResource) {
            if (!dto.getCode().matches("[A-Za-z][A-Za-z0-9:_.*-]{0,99}"))
                throw new BizException(400, "请输入合法的应用权限编码，不允许全局通配符");
            if (old != null && !dto.getCode().equals(old.get("code")))
                throw new BizException(400, "已发布的权限编码不可修改，请新建权限并重新授权");
            if (old == null)
                store.update(
                        "applicationRbacServiceUpdate36",
                        id,
                        app,
                        dto.getName(),
                        dto.getCode(),
                        dto.getDescription(),
                        dto.isAvailable(),
                        dto.getSort());
            else
                changed(
                        store.update(
                                "applicationRbacServiceUpdate37",
                                dto.getName(),
                                dto.getDescription(),
                                dto.isAvailable(),
                                dto.getSort(),
                                id,
                                app,
                                version(dto.getExpectedVersion())));
        } else {
            long parent = dto.getParentId() == null ? 0 : dto.getParentId();
            var visited = new HashSet<Long>();
            visited.add(id);
            for (long cursor = parent; cursor != 0; ) {
                if (!visited.add(cursor)) throw new BizException(400, "菜单父子关系不能形成循环");
                var node = store.one("applicationRbacServiceSelect11", cursor, app);
                if (number(node, "type") == 3) throw new BizException(400, "按钮不能包含子菜单");
                cursor = number(node, "parent_id");
            }
            if (!dto.getPermission().isBlank())
                store.one("applicationRbacServiceSelect12", dto.getPermission(), app);
            if (dto.getType() == 2 && !dto.getPath().matches("/[A-Za-z0-9_/-]+"))
                throw new BizException(400, "页面菜单必须填写应用内绝对路径");
            if (dto.getType() == 3
                    && !store.queryForList("applicationRbacServiceSelect21", id, app).isEmpty())
                throw new BizException(400, "含下级的菜单不能改为按钮");
            if (old == null)
                store.update(
                        "applicationRbacServiceUpdate38",
                        id,
                        app,
                        parent,
                        dto.getType(),
                        dto.getName(),
                        dto.getPath(),
                        dto.getComponent(),
                        dto.getIcon(),
                        dto.getPermission(),
                        dto.getRouteName(),
                        dto.getOpenMode(),
                        dto.isVisible(),
                        dto.isAvailable(),
                        dto.getSort());
            else
                changed(
                        store.update(
                                "applicationRbacServiceUpdate39",
                                parent,
                                dto.getType(),
                                dto.getName(),
                                dto.getPath(),
                                dto.getComponent(),
                                dto.getIcon(),
                                dto.getPermission(),
                                dto.getRouteName(),
                                dto.getOpenMode(),
                                dto.isVisible(),
                                dto.isAvailable(),
                                dto.getSort(),
                                id,
                                app,
                                version(dto.getExpectedVersion())));
        }
        revocations.application(app, "RESOURCE_CHANGED");
        return id;
    }

    @Transactional
    @OperateLog(module = "应用资源", action = "删除应用资源", type = OperateType.DELETE, maskParams = true)
    public void removeResource(long app, String kind, long id, int version) {
        boundary.require("manage");
        nonConsole(app);
        boolean permissionResource = permissions(kind);
        store.one("applicationRbacServiceSelect13", app);
        var old =
                store.one(
                        permissionResource ? "applicationPermission" : "applicationMenu", id, app);
        if ("menus".equals(kind)
                && !store.queryForList("applicationRbacServiceSelect22", app, id).isEmpty())
            throw new BizException(409, "请先删除下级菜单");
        if (permissionResource
                && (!store.queryForList("applicationRbacServiceSelect23", app, id).isEmpty()
                        || !store.queryForList(
                                        "applicationRbacServiceSelect24", app, old.get("code"))
                                .isEmpty())) throw new BizException(409, "权限仍被角色或菜单引用，请先解除关联");
        changed(
                store.update(
                        permissionResource
                                ? "deleteApplicationPermission"
                                : "deleteApplicationMenu",
                        id,
                        app,
                        version));
        revocations.application(app, "RESOURCE_DELETED");
    }
}
