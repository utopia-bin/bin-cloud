package cn.utopiabin.cloud.platform.spi.sms;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "短信供应商发送命令")
public record SmsSendCommand(
        @Schema(description = "接收短信的中国大陆手机号", example = "13800138000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,
        @Schema(description = "短信供应商模板编码", example = "SMS_123456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String templateCode,
        @Schema(description = "短信模板变量，键名须与供应商模板占位符一致", example = "{\"code\": \"123456\"}")
        Map<String, String> parameters) {
}
