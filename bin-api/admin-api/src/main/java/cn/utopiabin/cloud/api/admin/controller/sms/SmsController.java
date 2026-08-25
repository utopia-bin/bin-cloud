package cn.utopiabin.cloud.api.admin.controller.sms;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.sms.SmsApi;
import cn.utopiabin.cloud.platform.model.dto.sms.SmsCodeSendDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端短信验证码接口。 */
@Tag(name = "短信服务")
@RestController
@RequestMapping("/sms")
public class SmsController {

    @DubboReference
    private SmsApi smsApi;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/code")
    public RestResult<Void> sendVerificationCode(@Valid @RequestBody SmsCodeSendDTO dto) {
        smsApi.sendVerificationCode(dto);
        return RestResult.ok();
    }
}
