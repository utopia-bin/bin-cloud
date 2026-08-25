package cn.utopiabin.cloud.platform.model.dto.sms;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import cn.utopiabin.cloud.platform.model.enums.SmsScene;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "短信验证码发送参数")
public class SmsCodeSendDTO extends JsonSerializable {

    @NotBlank
    @Schema(description = "验证码业务所属租户编码", example = "default",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "接收验证码的中国大陆手机号", example = "13800138000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotNull
    @Schema(description = "验证码使用场景：REGISTER 注册、LOGIN 登录、RESET_PASSWORD 重置密码",
            example = "LOGIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private SmsScene scene;
}
