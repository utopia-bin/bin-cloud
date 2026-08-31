package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.*;

@Service
@RequiredArgsConstructor
public class ApplicationBoundary {
    private final ApplicationStore store;
    private final PermissionService permissions;

    public long userId() {
        try { return Long.parseLong(UserContextHolder.getUserId()); }
        catch (Exception e) { throw new BizException(401, "请先登录"); }
    }

    public long tenantId() {
        userId();
        try { return Long.parseLong(UserContextHolder.getTenantId()); }
        catch (Exception e) { throw new BizException(401, "租户上下文缺失"); }
    }

    public boolean global() { return permissions.hasPermission(userId(), "platform:application:provision"); }

    public void require(String action) {
        if (!permissions.hasPermission(userId(), "platform:application:" + action)) throw new BizException(403, "缺少应用" + action + "权限");
    }

    public long queryTenant(Long requested) {
        long own = tenantId();
        if (requested != null && requested != own && !global()) throw new BizException(403, "不能访问其他租户的应用数据");
        return requested == null ? own : requested;
    }

    public Map<String, Object> instance(long id, String action) {
        require(action);
        var row = store.one("SELECT * FROM sys_tenant_application WHERE id=? AND is_delete=0", id);
        queryTenant(number(row, "tenant_id"));
        return row;
    }

    public Map<String, Object> access(long tenant, long user, long instance) {
        var row = store.one("""
                SELECT ta.*, a.code AS application_code, a.name AS application_name, a.status AS product_status,
                       a.sso_enabled, a.service_id, a.entry_url, a.icon_url,
                       t.available AS tenant_available, t.is_delete AS tenant_deleted, t.expire_time AS tenant_expire,
                       u.id AS user_id, u.username, u.available AS user_available, u.is_delete AS user_deleted, u.credential_version
                FROM sys_tenant_application ta JOIN sys_application a ON a.id=ta.application_id AND a.is_delete=0
                JOIN sys_tenant t ON t.id=ta.tenant_id
                JOIN sys_user u ON u.tenant_id=ta.tenant_id AND u.id=?
                WHERE ta.id=? AND ta.tenant_id=? AND ta.is_delete=0
                """, user, instance, tenant);
        var now = LocalDateTime.now();
        if (!flag(row,"tenant_available") || number(row,"tenant_deleted") != 0
                || !flag(row,"user_available") || number(row,"user_deleted") != 0
                || !within(null,time(row,"tenant_expire"),now)
                || !"ENABLED".equals(row.get("product_status")) || !"ACTIVE".equals(row.get("status"))
                || !within(time(row,"effective_at"),time(row,"expire_at"),now)) {
            throw new BizException(403, "应用、租户、用户或开通实例已停用、尚未生效或已到期");
        }
        if (!java.util.List.of("ALL", "ASSIGNED").contains(row.get("access_policy"))) throw new BizException(403, "不支持的应用准入策略");
        if ("ASSIGNED".equals(row.get("access_policy"))) {
            var grant = store.jdbc().queryForList("SELECT * FROM sys_user_application WHERE tenant_id=? AND tenant_application_id=? AND user_id=? AND is_delete=0",
                    tenant, instance, user);
            if (grant.isEmpty() || !"ACTIVE".equals(grant.getFirst().get("status"))
                    || !within(time(grant.getFirst(),"effective_at"),time(grant.getFirst(),"expire_at"),now)) {
                throw new BizException(403, "尚未获得该应用的有效准入授权");
            }
            row.put("grant_expire", time(grant.getFirst(),"expire_at"));
        }
        return row;
    }
}
