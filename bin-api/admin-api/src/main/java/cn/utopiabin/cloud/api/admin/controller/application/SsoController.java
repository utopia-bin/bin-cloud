package cn.utopiabin.cloud.api.admin.controller.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.application.SsoApi;
import cn.utopiabin.cloud.platform.model.dto.application.SsoAuthorizeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoExchangeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoRefreshDTO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuthorizeVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sso")
@Tag(name = "应用单点登录")
public class SsoController {
    @DubboReference private SsoApi api;

    @PostMapping("/authorize")
    @Operation(summary = "使用平台登录态签发一次性授权码")
    public RestResult<SsoAuthorizeVO> authorize(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SsoAuthorizeDTO dto) {
        if (!authorization.startsWith("Bearer ")) throw new BizException(401, "缺少平台Token");
        return RestResult.ok(api.authorize(authorization.substring(7), dto));
    }

    @PostMapping("/token")
    @Operation(summary = "应用后端凭客户端凭证和PKCE兑换Token；禁止浏览器直接使用")
    public RestResult<SsoTokenVO> exchange(@Valid @RequestBody SsoExchangeDTO dto) {
        return RestResult.ok(api.exchange(dto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "应用后端轮换刷新令牌")
    public RestResult<SsoTokenVO> refresh(@Valid @RequestBody SsoRefreshDTO dto) {
        return RestResult.ok(api.refresh(dto));
    }
}
