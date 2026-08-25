package cn.utopiabin.cloud.platform.spi.sms;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "短信供应商发送结果")
public record SmsSendResult(
        @Schema(description = "短信是否成功提交至供应商", example = "true")
        boolean success,
        @Schema(description = "短信供应商返回的请求 ID，用于链路追踪", example = "req-20260825-001")
        String requestId,
        @Schema(description = "发送失败时供应商返回的错误编码", example = "INVALID_PHONE")
        String errorCode,
        @Schema(description = "发送失败原因；发送成功时为空", example = "手机号格式不正确")
        String errorMessage) {
    public static SmsSendResult success(String requestId) {
        return new SmsSendResult(true, requestId, null, null);
    }

    public static SmsSendResult failure(String errorCode, String errorMessage) {
        return new SmsSendResult(false, null, errorCode, errorMessage);
    }
}
