package cn.utopiabin.cloud.platform.api.sms;

import cn.utopiabin.cloud.platform.model.dto.sms.SmsCodeSendDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 可供其他模块调用的短信验证码 Dubbo 契约。 */
@Tag(name = "短信服务", description = "短信验证码发送能力")
public interface SmsApi {
    @Operation(summary = "发送短信验证码")
    void sendVerificationCode(SmsCodeSendDTO dto);
}
