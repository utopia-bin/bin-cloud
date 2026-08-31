package cn.utopiabin.cloud.platform.api.impl.auth;

import cn.utopiabin.cloud.platform.api.auth.AuthApi;
import cn.utopiabin.cloud.platform.model.dto.auth.ChangePasswordDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneLoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneRegisterDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneResetPasswordDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.CurrentUserVO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 认证授权 API 实现
 * <p>
 * 委托 {@link AuthService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@org.springframework.validation.annotation.Validated
@DubboService
@RequiredArgsConstructor
@Tag(name = "认证授权", description = "认证授权 Dubbo 服务实现")
public class AuthApiImpl implements AuthApi {

    private final AuthService authService;

    @Override
    public LoginResultVO login(LoginDTO dto) {
        return authService.login(dto);
    }

    @Override
    public LoginResultVO registerByPhone(PhoneRegisterDTO dto) {
        return authService.registerByPhone(dto);
    }

    @Override
    public LoginResultVO loginByPhone(PhoneLoginDTO dto) {
        return authService.loginByPhone(dto);
    }

    @Override
    public void resetPasswordByPhone(PhoneResetPasswordDTO dto) {
        authService.resetPasswordByPhone(dto);
    }

    @Override
    public void logout(String token) {
        authService.logout(token);
    }

    @Override
    public CurrentUserVO currentUser() {
        return authService.currentUser();
    }

    @Override
    public List<SysMenuTreeVO> currentUserMenus() {
        return authService.currentUserMenus();
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        authService.changePassword(dto);
    }
}
