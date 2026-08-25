package cn.utopiabin.cloud.platform.api.impl.sms;

import cn.utopiabin.cloud.platform.api.sms.SmsApi;
import cn.utopiabin.cloud.platform.model.dto.sms.SmsCodeSendDTO;
import cn.utopiabin.cloud.platform.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.validation.annotation.Validated;

/** 短信验证码 Dubbo 服务实现。 */
@Validated
@DubboService
@RequiredArgsConstructor
public class SmsApiImpl implements SmsApi {

    private final SmsService smsService;

    @Override
    public void sendVerificationCode(@Valid SmsCodeSendDTO dto) {
        smsService.sendVerificationCode(dto);
    }
}
