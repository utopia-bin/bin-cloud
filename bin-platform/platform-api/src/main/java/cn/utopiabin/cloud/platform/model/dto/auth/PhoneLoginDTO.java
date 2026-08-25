package cn.utopiabin.cloud.platform.model.dto.auth;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "手机号验证码登录参数")
public class PhoneLoginDTO extends JsonSerializable {

    @NotBlank
    @Schema(description = "登录租户编码", example = "default", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "接收登录验证码的中国大陆手机号", example = "13800138000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank
    @Size(min = 4, max = 8)
    @Schema(description = "短信登录验证码，长度为 4 至 8 位", example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
