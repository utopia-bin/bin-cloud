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
  /**
   * 校验应用访问令牌是否仍对应有效会话。
   *
   * <p>该方法供网关在转发请求前调用，不对外暴露 HTTP 端点。
   *
   * @param accessToken 应用访问令牌
   * @param expectedAudience 目标应用编码
   * @return 会话与令牌边界均有效时返回 {@code true}
   */
  boolean validateSession(String accessToken, String expectedAudience);

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
