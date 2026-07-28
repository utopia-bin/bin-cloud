package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.model.dto.auth.ChangePasswordDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.CurrentUserVO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import cn.utopiabin.cloud.platform.model.vo.iam.UserPermissionVO;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.util.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 认证授权服务
 * <p>
 * 封装登录、登出、获取当前用户、修改密码等业务逻辑。
 * 通过 {@link PermissionService} 获取缓存的用户权限，优化登录性能。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_BLACKLIST_PREFIX = "gateway:token:blacklist:";

    private final SysUserRepository userRepository;
    private final PermissionService permissionService;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenProperties jwtTokenProperties;
    private final RedisClient redisClient;
    private final PasswordEncoder passwordEncoder;

    // ==================== 登录 ====================

    /**
     * 账号密码登录
     * <p>
     * 性能优化: 用户验证 1 次 DB 查询 + 权限聚合 2 次 JOIN 查询 (缓存命中时 0 次)
     *
     * @param dto 登录参数
     * @return 登录结果 (含 Token、用户信息、角色、菜单树)
     */
    public LoginResultVO login(LoginDTO dto) {
        var username = dto.getUsername().trim();
        var password = dto.getPassword();

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BizException(PlatformErrorCode.PASSWORD_ERROR.getCode(),
                    PlatformErrorCode.PASSWORD_ERROR.getMsg());
        }

        var user = userRepository.getByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(PlatformErrorCode.PASSWORD_ERROR.getCode(),
                    PlatformErrorCode.PASSWORD_ERROR.getMsg());
        }

        if (!Boolean.TRUE.equals(user.getAvailable())) {
            throw new BizException(PlatformErrorCode.USER_DISABLED.getCode(),
                    PlatformErrorCode.USER_DISABLED.getMsg());
        }

        // 获取缓存的用户权限 (缓存命中时 0 次 DB 查询)
        var perm = permissionService.getUserPermissions(user.getId());
        var roleCodes = perm.getRoles().stream()
                .map(SysRoleVO::getCode)
                .toList();

        // 签发 JWT Token
        String token = jwtTokenService.generate(
                String.valueOf(user.getId()),
                user.getUsername(),
                String.valueOf(user.getTenantId()),
                roleCodes);

        // 组装返回结果
        var result = new LoginResultVO();
        result.setToken(token);
        result.setUser(user.copyTo(SysUserVO.class));
        result.setRoles(perm.getRoles());
        result.setMenus(perm.getMenuTree());

        log.info("用户登录成功: username={}, userId={}", username, user.getId());
        return result;
    }

    // ==================== 登出 ====================

    /**
     * 退出登录
     * <p>
     * 将 Token 加入 Redis 黑名单，gateway 将拦截已注销的 Token。
     *
     * @param token 原始 JWT Token
     */
    public void logout(String token) {
        var ctx = UserContextHolder.get();
        if (StrUtil.isNotBlank(token)) {
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
        String key = TOKEN_BLACKLIST_PREFIX + jwtTokenService.blacklistSuffix(token);
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
     *
     * @param dto 修改密码参数 (原密码 + 新密码)
     */
    public void changePassword(ChangePasswordDTO dto) {
        var ctx = requireUserContext();

        Long userId = Long.valueOf(ctx.getUserId());
        var user = userRepository.getOrThrow(userId);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(PlatformErrorCode.PASSWORD_WRONG.getCode(),
                    PlatformErrorCode.PASSWORD_WRONG.getMsg());
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.updateById(user);
        log.info("用户修改密码成功: userId={}", userId);
    }

    // ==================== 私有方法 ====================

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
