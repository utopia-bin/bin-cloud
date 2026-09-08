package cn.utopiabin.cloud.platform.service.tenant;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantAdminDTO;
import cn.utopiabin.cloud.platform.repository.iam.SysPermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRolePermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRoleRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.service.application.TenantApplicationService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 租户管理员初始化服务，仅由已授权的租户管理流程调用。 */
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {
  public static final Set<String> ADMIN_PERMISSIONS =
      Set.of(
          "platform:user:read",
          "platform:user:create",
          "platform:user:update",
          "platform:user:delete",
          "platform:user:assign-role",
          "platform:user:reset-password",
          "platform:role:read",
          "platform:role:create",
          "platform:role:update",
          "platform:role:delete",
          "platform:role:assign-permission",
          "platform:permission:read",
          "platform:dict:read",
          "platform:dict:create",
          "platform:dict:update",
          "platform:dict:delete",
          "platform:parameter:read",
          "platform:parameter:create",
          "platform:parameter:update",
          "platform:parameter:delete",
          "platform:operate-log:read",
          "platform:application:read",
          "platform:application:grant",
          "platform:application:role",
          "platform:application:audit",
          "platform:application:revoke");

  private final TenantRepository tenantRepository;
  private final SysUserRepository userRepository;
  private final SysRoleRepository roleRepository;
  private final SysUserRoleRepository userRoleRepository;
  private final SysRolePermissionRepository rolePermissionRepository;
  private final SysPermissionRepository permissionRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordValidator passwordValidator;
  private final TenantApplicationService tenantApplicationService;

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
    // 目标租户行锁串行化重试，不能因管理员角色被逻辑删除而重新开通。
    tenantRepository.lockExisting(tenantId);
    if (roleRepository.consoleRoleExistsIncludingDeleted(tenantId, "tenant_admin")) {
      throw new BizException(409, "该租户已初始化管理员，请通过用户管理维护，不能重复开通");
    }
    if (userRepository.initializationUserExists(tenantId, username, false)) {
      throw new BizException(409, "管理员账号已存在，请使用其他账号；不会重置已有用户密码");
    }
    var permissionIds = permissionRepository.listAvailableConsolePermissionIds(ADMIN_PERMISSIONS);
    if (permissionIds.size() != ADMIN_PERMISSIONS.size()) {
      throw new BizException(409, "租户基础权限缺失或已停用，请先检查数据库迁移及权限配置");
    }
    tenantApplicationService.ensureConsole(tenantId);
    long userId =
        userRepository.insertAdministrator(
            tenantId, username, passwordEncoder.encode(dto.getAdminPassword()), "租户管理员");
    long roleId = roleRepository.insertConsoleAdministrator(tenantId, "租户管理员", "tenant_admin");
    userRoleRepository.bindConsoleRole(tenantId, userId, roleId);
    rolePermissionRepository.grantConsolePermissions(tenantId, roleId, permissionIds);
  }
}
