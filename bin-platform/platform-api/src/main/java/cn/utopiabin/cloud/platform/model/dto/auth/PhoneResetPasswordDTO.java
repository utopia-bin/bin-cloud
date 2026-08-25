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
@Schema(description = "手机号验证码重置密码参数")
public class PhoneResetPasswordDTO extends JsonSerializable {

    @NotBlank
    @Schema(description = "账号所属租户编码", example = "default", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "待重置密码账号绑定的中国大陆手机号", example = "13800138000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank
    @Size(min = 4, max = 8)
    @Schema(description = "短信重置密码验证码，长度为 4 至 8 位", example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(description = "重置后的登录密码，长度为 8 至 100 个字符", example = "NewPassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
