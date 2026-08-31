package cn.utopiabin.cloud.api.open.controller;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.application.SsoApi;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationProfileVO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

@RestController
public class ApplicationProfileController {
    @DubboReference private SsoApi sso;
    @GetMapping("/application/profile")
    @Operation(summary="Bearer Token访问示例，固定验证learning-workbench受众")
    public RestResult<ApplicationProfileVO> profile(@RequestHeader("Authorization") String authorization) {
        if(!authorization.startsWith("Bearer ")) throw new BizException(401,"缺少应用Token");
        return RestResult.ok(sso.profile(authorization.substring(7),"learning-workbench"));
    }
}
