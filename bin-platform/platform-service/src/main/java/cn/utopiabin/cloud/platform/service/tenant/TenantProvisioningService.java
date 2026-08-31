package cn.utopiabin.cloud.platform.service.tenant;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantAdminDTO;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Explicit cross-tenant provisioning; called only by the authorized tenant service. */
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {
    public static final Set<String> ADMIN_PERMISSIONS = Set.of(
            "platform:user:read", "platform:user:create", "platform:user:update", "platform:user:delete",
            "platform:user:assign-role", "platform:user:reset-password",
            "platform:role:read", "platform:role:create", "platform:role:update", "platform:role:delete",
            "platform:role:assign-permission", "platform:permission:read",
            "platform:dict:read", "platform:dict:create", "platform:dict:update", "platform:dict:delete",
            "platform:parameter:read", "platform:parameter:create", "platform:parameter:update", "platform:parameter:delete",
            "platform:operate-log:read");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Transactional(propagation = Propagation.MANDATORY)
    public void provision(Long tenantId, TenantAdminDTO dto) {
        String username = dto.getAdminUsername();
        if (username == null || !username.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,49}")) {
            throw new BizException(400, "请填写合法的管理员账号");
        }
        passwordValidator.validate(dto.getAdminPassword());
        if (dto.getAdminPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BizException(400, "管理员密码 UTF-8 长度不能超过72字节");
        }
        // Lock the parent row to serialize concurrent retries, including soft-deleted markers.
        jdbc.queryForObject("SELECT id FROM sys_tenant WHERE id = ? AND is_delete = 0 FOR UPDATE", Long.class, tenantId);
        if (!jdbc.queryForList("SELECT id FROM sys_role WHERE tenant_id = ? AND code = 'tenant_admin'", tenantId).isEmpty()) {
            throw new BizException(409, "该租户已初始化管理员，请通过用户管理维护，不能重复开通");
        }
        if (!jdbc.queryForList("SELECT id FROM sys_user WHERE tenant_id = ? AND username = ? AND is_delete = 0",
                tenantId, username).isEmpty()) {
            throw new BizException(409, "管理员账号已存在，请使用其他账号；不会重置已有用户密码");
        }
        var permissions = jdbc.queryForList("SELECT id, code FROM sys_permission WHERE tenant_id IS NULL AND available = 1 AND is_delete = 0");
        var grants = permissions.stream().filter(p -> ADMIN_PERMISSIONS.contains(p.get("code"))).toList();
        if (grants.size() != ADMIN_PERMISSIONS.size()) {
            throw new BizException(409, "租户基础权限缺失或已停用，请先检查数据库迁移及权限配置");
        }
        long userId = IdWorker.getId();
        long roleId = IdWorker.getId();
        jdbc.update("INSERT INTO sys_user (id, tenant_id, username, password, real_name) VALUES (?, ?, ?, ?, '租户管理员')",
                userId, tenantId, username, passwordEncoder.encode(dto.getAdminPassword()));
        jdbc.update("INSERT INTO sys_role (id, tenant_id, name, code, data_scope) VALUES (?, ?, '租户管理员', 'tenant_admin', 1)", roleId, tenantId);
        jdbc.update("INSERT INTO sys_user_role (id, tenant_id, user_id, role_id) VALUES (?, ?, ?, ?)", IdWorker.getId(), tenantId, userId, roleId);
        for (var permission : grants) {
            jdbc.update("INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id) VALUES (?, ?, ?, ?)",
                    IdWorker.getId(), tenantId, roleId, permission.get("id"));
        }
    }
}
