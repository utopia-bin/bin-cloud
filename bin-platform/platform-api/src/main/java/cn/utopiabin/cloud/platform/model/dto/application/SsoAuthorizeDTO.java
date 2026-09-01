package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "一次性授权码签发参数")
public class SsoAuthorizeDTO extends JsonSerializable {
    @Schema(description = "要进入的租户应用实例")
    @NotNull
    @Positive
    private Long tenantApplicationId;

    @Schema(description = "必须精确命中该应用启用的回调白名单")
    @NotBlank
    @Size(max = 500)
    private String redirectUri;

    @Schema(description = "发起端随机CSRF状态，回调端必须校验")
    @NotBlank
    @Size(min = 32, max = 128)
    private String state;

    @Schema(description = "PKCE S256挑战，Base64URL无填充")
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{43}")
    private String codeChallenge;
}
