package cn.utopiabin.cloud.platform.service.bootstrap;

import cn.utopiabin.cloud.platform.config.PlatformBootstrapProperties;
import cn.utopiabin.cloud.platform.constant.PlatformConstants;
import cn.utopiabin.cloud.platform.repository.iam.SysMenuRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysPermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRolePermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRoleRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.service.application.TenantApplicationService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅供启动 Runner 调用的首次初始化，不提供公开 HTTP/Dubbo 入口。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformBootstrapService {

  private final TenantRepository tenantRepository;
  private final SysUserRepository userRepository;
  private final SysRoleRepository roleRepository;
  private final SysUserRoleRepository userRoleRepository;
  private final SysRolePermissionRepository rolePermissionRepository;
  private final SysPermissionRepository permissionRepository;
  private final SysMenuRepository menuRepository;
  private final PlatformBootstrapProperties properties;
  private final PasswordEncoder passwordEncoder;
  private final PasswordValidator passwordValidator;
  private final TenantApplicationService tenantApplicationService;

  @Transactional(rollbackFor = Exception.class)
  public boolean initialize() {
    if (!properties.isEnabled()) {
      return false;
    }

    // 全局权限唯一键与行锁串行化多实例启动；锁保持到整个初始化事务提交。
    var permission = permissionRepository.ensureAndLockPlatformWildcard();
    // 已禁用或已删除的角色仍是一次性标记，不能重新建立超级管理员。
    if (roleRepository.consoleRoleExistsIncludingDeleted(
        null, PlatformConstants.SUPER_ADMIN_ROLE_CODE)) {
      log.info("平台管理员初始化已跳过：已存在 super_admin 角色，未修改任何账号、密码或授权；请关闭 PLATFORM_BOOTSTRAP_ENABLED");
      return false;
    }
    if (permission.getTenantId() != null
        || !Boolean.TRUE.equals(permission.getAvailable())
        || !Integer.valueOf(0).equals(permission.getIsDelete())) {
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

    var tenant = tenantRepository.lockByCodeIncludingDeleted(tenantCode);
    long tenantId;
    if (tenant == null) {
      tenantId = tenantRepository.insertInitialTenant(tenantName.trim(), tenantCode);
    } else {
      if (!Boolean.TRUE.equals(tenant.getAvailable())
          || !Integer.valueOf(0).equals(tenant.getIsDelete())) {
        throw new IllegalStateException("目标租户已被禁用或删除，初始化不会修改其状态");
      }
      LocalDateTime expiry = tenant.getExpireTime();
      if (expiry != null && !expiry.isAfter(LocalDateTime.now())) {
        throw new IllegalStateException("目标租户已过期，初始化不会修改其有效期");
      }
      tenantId = tenant.getId();
    }
    if (userRepository.initializationUserExists(tenantId, username, true)) {
      throw new IllegalStateException("目标租户已存在同名账号，初始化拒绝重置密码或提升权限；请人工核对账号");
    }

    tenantApplicationService.ensureConsole(tenantId);
    long userId =
        userRepository.insertAdministrator(
            tenantId, username, passwordEncoder.encode(password), "平台管理员");
    long roleId =
        roleRepository.insertConsoleAdministrator(
            tenantId, "超级管理员", PlatformConstants.SUPER_ADMIN_ROLE_CODE);
    userRoleRepository.bindConsoleRole(tenantId, userId, roleId);
    rolePermissionRepository.grantConsolePermissions(tenantId, roleId, List.of(permission.getId()));

    // 为现有前端提供导航和权限投影，只补不存在的路径，不覆盖已有菜单配置。
    menuRepository.ensureInitialConsoleMenu(
        "租户管理", "/tenant", "OfficeBuilding", "platform:tenant:read", 10);
    menuRepository.ensureInitialConsoleMenu("用户管理", "/iam/users", "User", "platform:user:read", 20);
    menuRepository.ensureInitialConsoleMenu(
        "角色管理", "/iam/roles", "UserFilled", "platform:role:read", 30);
    menuRepository.ensureInitialConsoleMenu(
        "权限管理", "/iam/permissions", "Key", "platform:permission:read", 40);
    menuRepository.ensureInitialConsoleMenu("菜单管理", "/iam/menus", "Menu", "platform:menu:read", 50);
    menuRepository.ensureInitialConsoleMenu(
        "字典管理", "/system/dicts", "Collection", "platform:dict:read", 60);
    menuRepository.ensureInitialConsoleMenu(
        "参数管理", "/system/parameters", "Setting", "platform:parameter:read", 70);
    menuRepository.ensureInitialConsoleMenu(
        "操作日志", "/system/operate-logs", "Document", "platform:operate-log:read", 80);
    // Runner 在事务代理返回（提交完成）后才记录成功，不记录密码或其哈希。
    return true;
  }

  private static String identifier(String value, String label) {
    if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,49}")) {
      throw new IllegalStateException(label + "必须为 1 至 50 位字母、数字、下划线、点或连字符，且以字母或数字开头");
    }
    return value;
  }
}
