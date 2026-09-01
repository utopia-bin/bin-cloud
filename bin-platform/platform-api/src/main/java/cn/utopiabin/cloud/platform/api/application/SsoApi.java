package cn.utopiabin.cloud.platform.api.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.model.dto.application.SsoAuthorizeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoExchangeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoRefreshDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationProfileVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuthorizeVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

public interface SsoApi {
    @Operation(summary = "SSO authorize")
    SsoAuthorizeVO authorize(String platformToken, @Valid SsoAuthorizeDTO dto) throws BizException;

    @Operation(summary = "SSO exchange")
    SsoTokenVO exchange(@Valid SsoExchangeDTO dto) throws BizException;

    @Operation(summary = "SSO refresh")
    SsoTokenVO refresh(@Valid SsoRefreshDTO dto) throws BizException;

    @Operation(summary = "SSO profile")
    ApplicationProfileVO profile(String accessToken, String expectedAudience) throws BizException;

    @Operation(summary = "SSO logout")
    void logout(String accessToken, boolean global) throws BizException;
}
