package cn.utopiabin.cloud.platform.api.impl.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.application.SsoAuthorizeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoExchangeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoRefreshDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationProfileVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuthorizeVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import cn.utopiabin.cloud.platform.api.application.SsoApi;
import cn.utopiabin.cloud.platform.service.application.SsoService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.validation.annotation.Validated;

@DubboService
@Validated
@RequiredArgsConstructor
public class SsoApiImpl implements SsoApi {
    private final SsoService service;
    @Override public SsoAuthorizeVO authorize(String platformToken, SsoAuthorizeDTO dto) { return service.authorize(platformToken,dto); }
    @Override public SsoTokenVO exchange(SsoExchangeDTO dto) { return service.exchange(dto); }
    @Override public SsoTokenVO refresh(SsoRefreshDTO dto) { return service.refresh(dto); }
    @Override public ApplicationProfileVO profile(String accessToken, String expectedAudience) { return service.profile(accessToken,expectedAudience); }
    @Override public void logout(String accessToken, boolean global) { service.logout(accessToken,global); }
}
