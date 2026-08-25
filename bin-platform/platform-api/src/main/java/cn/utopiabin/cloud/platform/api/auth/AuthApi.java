package cn.utopiabin.cloud.platform.api.auth;

import cn.utopiabin.cloud.platform.model.dto.auth.ChangePasswordDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneLoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneRegisterDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneResetPasswordDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.CurrentUserVO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 认证授权 Dubbo API
 * <p>
 * 提供登录、登出、获取当前用户信息、修改密码等能力，与 gateway 的 JwtAuthFilter 联动：
 * <ul>
 *   <li>登录 → 签发 JWT → gateway 验签放行</li>
 *   <li>登出 → Token 加入 Redis 黑名单 → gateway 拦截</li>
 *   <li>currentUser / currentUserMenus → 基于网关注入的 X-User-Id 头查询</li>
 *   <li>changePassword → 校验原密码后更新，可选强制重新登录</li>
 * </ul>
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "认证授权", description = "登录、登出、获取当前用户信息及菜单权限、修改密码")
public interface AuthApi {

    @Operation(summary = "账号密码登录", description = "验证租户编码、用户名和密码，签发JWT Token，返回用户信息、角色及菜单树")
    LoginResultVO login(@Parameter(description = "登录参数", required = true) LoginDTO dto);

    @Operation(summary = "手机号注册", description = "校验注册验证码并创建用户，注册成功后签发JWT Token")
    LoginResultVO registerByPhone(PhoneRegisterDTO dto);

    @Operation(summary = "手机号验证码登录", description = "校验登录验证码并签发JWT Token")
    LoginResultVO loginByPhone(PhoneLoginDTO dto);

    @Operation(summary = "手机号重置密码", description = "校验重置密码验证码后更新密码")
    void resetPasswordByPhone(PhoneResetPasswordDTO dto);

    @Operation(summary = "退出登录", description = "将Token加入Redis黑名单，gateway将拦截已注销的Token")
    void logout(@Parameter(description = "原始JWT Token（由HTTP Controller从请求头提取传入）") String token);

    @Operation(summary = "获取当前登录用户完整信息", description = "返回用户信息、角色列表、菜单树")
    CurrentUserVO currentUser();

    @Operation(summary = "获取当前用户的菜单树", description = "仅返回菜单树结构，用于前端动态路由渲染")
    List<SysMenuTreeVO> currentUserMenus();

    @Operation(summary = "修改密码", description = "校验原密码后更新为新密码，基于 UserContext 获取当前用户")
    void changePassword(@Parameter(description = "修改密码参数", required = true) ChangePasswordDTO dto);
}
