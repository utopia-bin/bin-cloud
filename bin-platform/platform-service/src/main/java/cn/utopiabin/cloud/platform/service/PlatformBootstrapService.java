package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.platform.config.PlatformBootstrapProperties;
import cn.utopiabin.cloud.platform.constant.PlatformConstants;
import cn.utopiabin.cloud.platform.service.application.TenantApplicationService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/** 仅供启动 Runner 调用的首次初始化，不提供公开 HTTP/Dubbo 入口。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformBootstrapService {

    private final JdbcTemplate jdbc;
    private final PlatformBootstrapProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TenantApplicationService tenantApplicationService;

    @Transactional(rollbackFor = Exception.class)
    public boolean initialize() {
        if (!properties.isEnabled()) {
            return false;
        }

        // 不更新已有权限；唯一键及行锁将同一数据库上的并发初始化串行化，锁保持到事务结束。
        jdbc.update(
                """
                INSERT INTO sys_permission (id, name, code, description)
                VALUES (?, '全部权限', '*', '仅供平台内置超级管理员使用')
                ON DUPLICATE KEY UPDATE code = code
                """,
                IdWorker.getId());
        var permission =
                jdbc.queryForMap(
                        """
                        SELECT id, tenant_id, available, is_delete FROM sys_permission WHERE application_id=1 AND code = '*' FOR UPDATE
                        """);

        // 角色作为一次性标记：包括禁用/软删除的角色，避免修改配置后意外创建第二个超级管理员。
        var existingRoles =
                jdbc.queryForList(
                        """
                        SELECT id FROM sys_role WHERE application_id=1 AND code = ? FOR UPDATE
                        """,
                        PlatformConstants.SUPER_ADMIN_ROLE_CODE);
        if (!existingRoles.isEmpty()) {
            log.info("平台管理员初始化已跳过：已存在 super_admin 角色，未修改任何账号、密码或授权；请关闭 PLATFORM_BOOTSTRAP_ENABLED");
            return false;
        }
        if (permission.get("tenant_id") != null
                || !flag(permission.get("available"))
                || flag(permission.get("is_delete"))) {
            throw new IllegalStateException("通配权限必须是启用且未删除的全局权限，请人工核对后再初始化");
        }

        String tenantCode = identifier(properties.getTenantCode(), "租户编码");
        String username = identifier(properties.getUsername(), "管理员用户名");
        String tenantName = properties.getTenantName();
        if (tenantName == null || tenantName.isBlank() || tenantName.length() > 100) {
            throw new IllegalStateException("初始化租户名称必须为 1 至 100 个字符");
        }
        String password = properties.getPassword();
        if (password == null
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "请设置 PLATFORM_BOOTSTRAP_PASSWORD：至少 12 个字符，UTF-8 编码不超过 72 字节");
        }
        passwordValidator.validate(password);

        var tenants =
                jdbc.queryForList(
                        """
                        SELECT id, available, is_delete, expire_time FROM sys_tenant WHERE code = ? FOR UPDATE
                        """,
                        tenantCode);
        long tenantId;
        if (tenants.isEmpty()) {
            tenantId = IdWorker.getId();
            jdbc.update(
                    "INSERT INTO sys_tenant (id, name, code) VALUES (?, ?, ?)",
                    tenantId,
                    tenantName.trim(),
                    tenantCode);
        } else {
            var tenant = tenants.getFirst();
            if (!flag(tenant.get("available")) || flag(tenant.get("is_delete"))) {
                throw new IllegalStateException("目标租户已被禁用或删除，初始化不会修改其状态");
            }
            LocalDateTime expiry =
                    jdbc.queryForObject(
                            "SELECT expire_time FROM sys_tenant WHERE id = ?",
                            LocalDateTime.class,
                            tenant.get("id"));
            if (expiry != null && !expiry.isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("目标租户已过期，初始化不会修改其有效期");
            }
            tenantId = ((Number) tenant.get("id")).longValue();
        }
        if (!jdbc.queryForList(
                        "SELECT id FROM sys_user WHERE tenant_id = ? AND username = ? FOR UPDATE",
                        tenantId,
                        username)
                .isEmpty()) {
            throw new IllegalStateException("目标租户已存在同名账号，初始化拒绝重置密码或提升权限；请人工核对账号");
        }

        tenantApplicationService.ensureConsole(tenantId);
        long userId = IdWorker.getId();
        long roleId = IdWorker.getId();
        jdbc.update(
                """
                INSERT INTO sys_user (id, tenant_id, username, password, real_name)
                VALUES (?, ?, ?, ?, '平台管理员')
                """,
                userId,
                tenantId,
                username,
                passwordEncoder.encode(password));
        jdbc.update(
                """
                INSERT INTO sys_role (id, tenant_id, tenant_application_id, name, code, data_scope)
                VALUES (?, ?, ?, '超级管理员', ?, 1)
                """,
                roleId,
                tenantId,
                tenantId,
                PlatformConstants.SUPER_ADMIN_ROLE_CODE);
        jdbc.update(
                "INSERT INTO sys_user_role (id, tenant_id, tenant_application_id, user_id, role_id)"
                    + " VALUES (?, ?, ?, ?, ?)",
                IdWorker.getId(),
                tenantId,
                tenantId,
                userId,
                roleId);
        jdbc.update(
                "INSERT INTO sys_role_permission (id, tenant_id, tenant_application_id, role_id,"
                        + " permission_id) VALUES (?, ?, ?, ?, ?)",
                IdWorker.getId(),
                tenantId,
                tenantId,
                roleId,
                permission.get("id"));

        // 为现有前端提供导航和权限投影，只补不存在的路径，不覆盖已有菜单配置。
        menu("租户管理", "/tenant", "OfficeBuilding", "platform:tenant:read", 10);
        menu("用户管理", "/iam/users", "User", "platform:user:read", 20);
        menu("角色管理", "/iam/roles", "UserFilled", "platform:role:read", 30);
        menu("权限管理", "/iam/permissions", "Key", "platform:permission:read", 40);
        menu("菜单管理", "/iam/menus", "Menu", "platform:menu:read", 50);
        menu("字典管理", "/system/dicts", "Collection", "platform:dict:read", 60);
        menu("参数管理", "/system/parameters", "Setting", "platform:parameter:read", 70);
        menu("操作日志", "/system/operate-logs", "Document", "platform:operate-log:read", 80);
        // Runner 在事务代理返回（提交完成）后才记录成功，不记录密码或其哈希。
        return true;
    }

    private void menu(String name, String path, String icon, String permission, int sort) {
        // 初始化已由全局权限行锁串行化；分开查询和插入，避免 MySQL 的同表子查询写入限制。
        if (!jdbc.queryForList(
                        "SELECT id FROM sys_menu WHERE application_id=1 AND path = ? FOR UPDATE",
                        path)
                .isEmpty()) {
            return;
        }
        jdbc.update(
                """
                INSERT INTO sys_menu (id, type, name, path, icon, permission, sort)
                VALUES (?, 2, ?, ?, ?, ?, ?)
                """,
                IdWorker.getId(),
                name,
                path,
                icon,
                permission,
                sort);
    }

    private static String identifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,49}")) {
            throw new IllegalStateException(label + "必须为 1 至 50 位字母、数字、下划线、点或连字符，且以字母或数字开头");
        }
        return value;
    }

    private static boolean flag(Object value) {
        return Boolean.TRUE.equals(value)
                || value instanceof Number number && number.intValue() != 0;
    }
}
