package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.common.annotations.RepeatSubmit;
import cn.utopiabin.cloud.common.annotations.DistributedLock;
import cn.utopiabin.cloud.platform.annotation.TenantIgnore;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.model.dto.auth.ChangePasswordDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneLoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneRegisterDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneResetPasswordDTO;
import cn.utopiabin.cloud.platform.model.enums.SmsScene;
import cn.utopiabin.cloud.platform.model.vo.auth.CurrentUserVO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.model.vo.auth.PasswordPolicyVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import cn.utopiabin.cloud.platform.model.vo.iam.UserPermissionVO;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.util.JwtTokenService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证授权服务
 * <p>
 * 封装登录、登出、获取当前用户、修改密码等业务逻辑。
 * <p>
 * 生产级安全能力:
 * <ul>
 *   <li>登录失败计数 + 账号锁定 (Redis, 可配置阈值/时长/窗口)</li>
 *   <li>登录失败递增延迟 (防高速撞库)</li>
 *   <li>登录时租户状态校验 (禁用/过期的租户禁止登录)</li>
 *   <li>修改密码强度校验</li>
 *   <li>登录防重复提交</li>
 *   <li>操作日志审计</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_BLACKLIST_PREFIX = "gateway:token:blacklist:";

    /** 登录失败计数 Key 前缀 */
    private static final String LOGIN_FAIL_COUNT_KEY = "platform:login:fail:";

    /** 账号锁定 Key 前缀 */
    private static final String LOGIN_LOCK_KEY = "platform:login:lock:";

    private final SysUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PermissionService permissionService;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenProperties jwtTokenProperties;
    private final RedisClient redisClient;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityProperties loginSecurityProperties;
    private final PasswordValidator passwordValidator;
    private final SmsService smsService;
    private final cn.utopiabin.cloud.platform.service.application.SsoService ssoService;
    private final cn.utopiabin.cloud.platform.service.application.ApplicationRevocationService sessionRevocations;

    public PasswordPolicyVO passwordPolicy() {
        return passwordValidator.policy();
    }

    // ==================== 登录 ====================

    /**
     * 账号密码登录
     * <p>
     * 安全流程: 锁定检查 → 密码校验(失败计数/递增延迟/阈值锁定) → 账号状态
     * → 租户状态 → 失败记录清零 → 权限聚合(缓存) → 签发 Token。
     * <p>
     * 性能优化: 租户与用户验证各 1 次 DB 查询 + 权限聚合 2 次 JOIN 查询 (缓存命中时 0 次)
     *
     * @param dto 登录参数
     * @return 登录结果 (含 Token、用户信息、角色、菜单树)
     */
    @TenantIgnore
    @RepeatSubmit(interval = 2, message = "登录请求过于频繁，请稍后再试")
    @OperateLog(module = "认证管理", action = "用户登录", type = OperateType.AUTH,
            principalSpel = "#dto.username", maskParams = true)
    public LoginResultVO login(LoginDTO dto) {
        var tenantCode = dto.getTenantCode() == null ? "" : dto.getTenantCode().trim();
        var username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        var password = dto.getPassword();

        if (StrUtil.isBlank(tenantCode) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BizException(PlatformErrorCode.PASSWORD_EMPTY.getCode(),
                    PlatformErrorCode.PASSWORD_EMPTY.getMsg());
        }

        var tenant = tenantRepository.getByCode(tenantCode);
        validateTenant(tenant);
        String loginIdentity = tenant.getId() + ":" + username;

        // 1. 账号锁定检查 (锁定期间直接拒绝，不消耗 DB 查询)
        assertNotLocked(loginIdentity, username);

        // 2. 密码校验 (失败走计数/延迟/锁定逻辑)
        var user = userRepository.getByTenantIdAndUsername(tenant.getId(), username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            recordLoginFailure(loginIdentity, username);
            throw new BizException(PlatformErrorCode.PASSWORD_ERROR.getCode(),
                    PlatformErrorCode.PASSWORD_ERROR.getMsg());
        }

        // 3. 账号状态校验
        if (!Boolean.TRUE.equals(user.getAvailable())) {
            throw new BizException(PlatformErrorCode.USER_DISABLED.getCode(),
                    PlatformErrorCode.USER_DISABLED.getMsg());
        }

        // 4. 租户状态校验 (禁用/过期的租户禁止登录)
        if (!tenant.getId().equals(user.getTenantId())) {
            throw new BizException(PlatformErrorCode.PASSWORD_ERROR.getCode(),
                    PlatformErrorCode.PASSWORD_ERROR.getMsg());
        }

        // 5. 登录成功，清除失败记录
        clearLoginFailure(loginIdentity);

        LoginResultVO result = issueLoginResult(user);
        log.info("用户登录成功: username={}, userId={}", username, user.getId());
        return result;
    }

    /** 手机号注册；手机号同时作为初始用户名，注册后直接签发登录令牌。 */
    @TenantIgnore
    @Transactional
    @DistributedLock(key = "'phone:register:' + #dto.tenantCode + ':' + #dto.phone")
    @RepeatSubmit(interval = 2, message = "注册请求过于频繁，请稍后再试")
    @OperateLog(module = "认证管理", action = "手机号注册", type = OperateType.AUTH, maskParams = true)
    public LoginResultVO registerByPhone(PhoneRegisterDTO dto) {
        String phone = trim(dto.getPhone());
        Tenant tenant = tenantRepository.getByCode(trim(dto.getTenantCode()));
        validateTenant(tenant);
        if (userRepository.getByTenantIdAndPhone(tenant.getId(), phone) != null) {
            throw biz(PlatformErrorCode.PHONE_DUPLICATE);
        }
        if (userRepository.getByTenantIdAndUsername(tenant.getId(), phone) != null) {
            throw biz(PlatformErrorCode.USER_DUPLICATE);
        }

        passwordValidator.validate(dto.getPassword());
        smsService.verifyAndConsume(tenant.getId(), phone, SmsScene.REGISTER, dto.getCode());

        SysUser user = new SysUser();
        user.setTenantId(tenant.getId());
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName("");
        user.setPhone(phone);
        user.setEmail("");
        user.setGender(0);
        user.setAvailable(true);
        user.setSort(10);
        user.setComment("");
        userRepository.save(user);

        log.info("手机号注册成功: tenantId={}, userId={}", tenant.getId(), user.getId());
        return issueLoginResult(user);
    }

    /** 短信验证码登录。 */
    @TenantIgnore
    @RepeatSubmit(interval = 2, message = "登录请求过于频繁，请稍后再试")
    @OperateLog(module = "认证管理", action = "手机号登录", type = OperateType.AUTH, maskParams = true)
    public LoginResultVO loginByPhone(PhoneLoginDTO dto) {
        String phone = trim(dto.getPhone());
        Tenant tenant = tenantRepository.getByCode(trim(dto.getTenantCode()));
        validateTenant(tenant);
        SysUser user = userRepository.getByTenantIdAndPhone(tenant.getId(), phone);
        if (user == null) {
            throw biz(PlatformErrorCode.PHONE_NOT_REGISTERED);
        }
        if (!Boolean.TRUE.equals(user.getAvailable())) {
            throw biz(PlatformErrorCode.USER_DISABLED);
        }

        smsService.verifyAndConsume(tenant.getId(), phone, SmsScene.LOGIN, dto.getCode());
        log.info("手机号登录成功: tenantId={}, userId={}", tenant.getId(), user.getId());
        return issueLoginResult(user);
    }

    /** 使用短信验证码重置密码。 */
    @TenantIgnore
    @Transactional
    @RepeatSubmit(interval = 2, message = "重置密码请求过于频繁，请稍后再试")
    @OperateLog(module = "认证管理", action = "手机号重置密码", type = OperateType.AUTH, maskParams = true)
    public void resetPasswordByPhone(PhoneResetPasswordDTO dto) {
        String phone = trim(dto.getPhone());
        Tenant tenant = tenantRepository.getByCode(trim(dto.getTenantCode()));
        validateTenant(tenant);
        SysUser user = userRepository.getByTenantIdAndPhone(tenant.getId(), phone);
        if (user == null) {
            throw biz(PlatformErrorCode.PHONE_NOT_REGISTERED);
        }
        if (!Boolean.TRUE.equals(user.getAvailable())) {
            throw biz(PlatformErrorCode.USER_DISABLED);
        }

        passwordValidator.validate(dto.getNewPassword());
        smsService.verifyAndConsume(tenant.getId(), phone, SmsScene.RESET_PASSWORD, dto.getCode());
        user.setCredentialVersion(java.util.Optional.ofNullable(user.getCredentialVersion()).orElse(0) + 1);
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        if (!userRepository.updateById(user)) throw new BizException(409, "用户已变化，请重新操作");
        sessionRevocations.user(tenant.getId(), user.getId(), "PASSWORD_CHANGED");
        log.info("手机号重置密码成功: tenantId={}, userId={}", tenant.getId(), user.getId());
    }

    // ==================== 登出 ====================

    /**
     * 退出登录
     * <p>
     * 将 Token 加入 Redis 黑名单，gateway 将拦截已注销的 Token。
     *
     * @param token 原始 JWT Token
     */
    @OperateLog(module = "认证管理", action = "用户登出", type = OperateType.AUTH, maskParams = true)
    public void logout(String token) {
        var ctx = UserContextHolder.get();
        if (StrUtil.isNotBlank(token)) {
            ssoService.logout(token, true);
            addToBlacklist(token);
        }
        if (ctx != null) {
            log.info("用户登出: userId={}", ctx.getUserId());
        }
    }

    private void addToBlacklist(String token) {
        if (!jwtTokenProperties.isTokenBlacklistEnabled() || StrUtil.isBlank(token)) {
            return;
        }
        long remainingTtl = jwtTokenService.getRemainingTtl(token);
        if (remainingTtl <= 0) {
            return;
        }
        String key = TOKEN_BLACKLIST_PREFIX + jwtTokenService.blacklistKeyDigest(token);
        redisClient.set(key, "1", Duration.ofSeconds(remainingTtl));
        log.info("Token 已加入黑名单: key={}, ttl={}s", key, remainingTtl);
    }

    // ==================== 当前用户 ====================

    /**
     * 获取当前登录用户完整信息
     * <p>
     * 基于 UserContext 获取 userId，从缓存加载权限数据。
     *
     * @return 当前用户信息 (含角色、菜单树)
     */
    public CurrentUserVO currentUser() {
        var ctx = requireUserContext();

        Long userId = Long.valueOf(ctx.getUserId());
        var user = userRepository.getOrThrow(userId);
        var perm = permissionService.getUserPermissions(userId);

        var vo = new CurrentUserVO();
        vo.setUser(user.copyTo(SysUserVO.class));
        vo.setRoles(perm.getRoles());
        vo.setPermissionCodes(perm.getPermissionCodes());
        vo.setMenus(perm.getMenuTree());
        return vo;
    }

    /**
     * 获取当前用户的菜单树
     *
     * @return 菜单树
     */
    public List<SysMenuTreeVO> currentUserMenus() {
        var ctx = UserContextHolder.get();
        if (ctx == null || StrUtil.isBlank(ctx.getUserId())) {
            return List.of();
        }

        Long userId = Long.valueOf(ctx.getUserId());
        UserPermissionVO perm = permissionService.getUserPermissions(userId);
        return perm.getMenuTree();
    }

    // ==================== 修改密码 ====================

    /**
     * 修改当前用户密码
     * <p>
     * 生产级校验: 原密码校验 + 新密码强度校验。
     *
     * @param dto 修改密码参数 (原密码 + 新密码)
     */
    @Transactional
    @OperateLog(module = "认证管理", action = "修改密码", type = OperateType.AUTH, maskParams = true)
    public void changePassword(ChangePasswordDTO dto) {
        var ctx = requireUserContext();

        Long userId = Long.valueOf(ctx.getUserId());
        var user = userRepository.getOrThrow(userId);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(PlatformErrorCode.PASSWORD_WRONG.getCode(),
                    PlatformErrorCode.PASSWORD_WRONG.getMsg());
        }

        // 新密码强度校验
        passwordValidator.validate(dto.getNewPassword());

        user.setCredentialVersion(java.util.Optional.ofNullable(user.getCredentialVersion()).orElse(0) + 1);

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        if (!userRepository.updateById(user)) throw new BizException(409, "用户已变化，请重新操作");
        sessionRevocations.user(user.getTenantId(), userId, "PASSWORD_CHANGED");
        log.info("用户修改密码成功: userId={}", userId);
    }

    // ==================== 登录安全私有方法 ====================

    /**
     * 账号锁定检查
     */
    private void assertNotLocked(String loginIdentity, String username) {
        if (redisClient.hasKey(LOGIN_LOCK_KEY + loginIdentity)) {
            log.warn("账号已锁定, 拒绝登录: username={}", username);
            throw new BizException(PlatformErrorCode.ACCOUNT_LOCKED.getCode(),
                    PlatformErrorCode.ACCOUNT_LOCKED.getMsg());
        }
    }

    /**
     * 记录登录失败: 计数 + 递增延迟 + 阈值锁定
     */
    private void recordLoginFailure(String loginIdentity, String username) {
        String failKey = LOGIN_FAIL_COUNT_KEY + loginIdentity;
        Long fails = redisClient.incr(failKey);
        long count = fails == null ? 1L : fails;

        // 首次失败设置计数窗口
        if (count == 1L) {
            redisClient.expire(failKey, Duration.ofSeconds(loginSecurityProperties.getFailCountWindowSeconds()));
        }

        // 递增延迟: 失败次数越多等待越久 (封顶 maxDelayMs)，提高撞库成本
        long delay = Math.min(count * loginSecurityProperties.getDelayBaseMs(),
                loginSecurityProperties.getMaxDelayMs());
        sleepQuietly(delay);

        // 达到阈值 → 锁定账号
        if (count >= loginSecurityProperties.getMaxFailCount()) {
            long lockSeconds = loginSecurityProperties.getLockDurationSeconds();
            redisClient.set(LOGIN_LOCK_KEY + loginIdentity, "1", Duration.ofSeconds(lockSeconds));
            redisClient.delete(failKey);
            log.warn("账号触发锁定保护: username={}, failCount={}, lockSeconds={}",
                    username, count, lockSeconds);
        } else {
            log.info("登录失败: username={}, failCount={}/{}, delay={}ms",
                    username, count, loginSecurityProperties.getMaxFailCount(), delay);
        }
    }

    /**
     * 登录成功后清除失败计数与锁定标记
     */
    private void clearLoginFailure(String loginIdentity) {
        redisClient.delete(LOGIN_FAIL_COUNT_KEY + loginIdentity);
        redisClient.delete(LOGIN_LOCK_KEY + loginIdentity);
    }

    /**
     * 租户状态校验: 禁用/过期/不存在的租户禁止登录
     */
    private void validateTenant(Tenant tenant) {
        if (tenant == null) {
            throw new BizException(PlatformErrorCode.TENANT_NOT_FOUND.getCode(),
                    PlatformErrorCode.TENANT_NOT_FOUND.getMsg());
        }
        if (!Boolean.TRUE.equals(tenant.getAvailable())) {
            throw new BizException(PlatformErrorCode.TENANT_DISABLED.getCode(),
                    PlatformErrorCode.TENANT_DISABLED.getMsg());
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(PlatformErrorCode.TENANT_EXPIRED.getCode(),
                    PlatformErrorCode.TENANT_EXPIRED.getMsg());
        }
    }

    private LoginResultVO issueLoginResult(SysUser user) {
        var perm = permissionService.getUserPermissions(user.getId());
        var roleCodes = perm.getRoles().stream()
                .map(SysRoleVO::getCode)
                .toList();
        String token = ssoService.platformLogin(user, roleCodes);

        var result = new LoginResultVO();
        result.setToken(token);
        result.setUser(user.copyTo(SysUserVO.class));
        result.setRoles(perm.getRoles());
        result.setPermissionCodes(perm.getPermissionCodes());
        result.setMenus(perm.getMenuTree());
        return result;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static BizException biz(PlatformErrorCode error) {
        return new BizException(error.getCode(), error.getMsg());
    }

    /**
     * 静默休眠 (防撞库延迟)，中断时恢复中断标记
     */
    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取当前用户上下文，未登录则抛出异常
     */
    private UserContext requireUserContext() {
        var ctx = UserContextHolder.get();
        if (ctx == null || StrUtil.isBlank(ctx.getUserId())) {
            throw new BizException(PlatformErrorCode.UNAUTHORIZED.getCode(),
                    PlatformErrorCode.UNAUTHORIZED.getMsg());
        }
        return ctx;
    }
}
