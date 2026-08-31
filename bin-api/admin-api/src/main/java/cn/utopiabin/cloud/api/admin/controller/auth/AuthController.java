package cn.utopiabin.cloud.api.admin.controller.auth;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.auth.AuthApi;
import cn.utopiabin.cloud.platform.model.dto.auth.ChangePasswordDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneLoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneRegisterDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneResetPasswordDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.CurrentUserVO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.model.vo.auth.PasswordPolicyVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端认证接口。 */
@Tag(name = "认证授权")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @DubboReference
    private AuthApi authApi;

    @Operation(summary = "获取当前密码设置策略")
    @GetMapping("/password-policy")
    public RestResult<PasswordPolicyVO> passwordPolicy() {
        return RestResult.ok(authApi.passwordPolicy());
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public RestResult<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return RestResult.ok(authApi.login(dto));
    }

    @Operation(summary = "手机号注册")
    @PostMapping("/phone/register")
    public RestResult<LoginResultVO> registerByPhone(@Valid @RequestBody PhoneRegisterDTO dto) {
        return RestResult.ok(authApi.registerByPhone(dto));
    }

    @Operation(summary = "手机号验证码登录")
    @PostMapping("/phone/login")
    public RestResult<LoginResultVO> loginByPhone(@Valid @RequestBody PhoneLoginDTO dto) {
        return RestResult.ok(authApi.loginByPhone(dto));
    }

    @Operation(summary = "手机号重置密码")
    @PutMapping("/phone/password")
    public RestResult<Void> resetPasswordByPhone(@Valid @RequestBody PhoneResetPasswordDTO dto) {
        authApi.resetPasswordByPhone(dto);
        return RestResult.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public RestResult<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authApi.logout(extractBearerToken(authorization));
        return RestResult.ok();
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public RestResult<CurrentUserVO> currentUser() {
        return RestResult.ok(authApi.currentUser());
    }

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/me/menus")
    public RestResult<List<SysMenuTreeVO>> currentUserMenus() {
        return RestResult.ok(authApi.currentUserMenus());
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/me/password")
    public RestResult<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authApi.changePassword(dto);
        return RestResult.ok();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim()
                : authorization.trim();
    }
}
