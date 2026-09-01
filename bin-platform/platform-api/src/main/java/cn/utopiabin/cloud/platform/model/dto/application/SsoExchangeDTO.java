package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "仅应用后端使用的授权码兑换")
public class SsoExchangeDTO extends JsonSerializable {
    @Schema(description = "登记的serviceId")
    @NotBlank
    @Size(max = 100)
    private String clientId;

    @Schema(description = "应用后端持有的客户端凭证，不下发浏览器")
    @NotBlank
    @Size(max = 200)
    private String clientSecret;

    @Schema(description = "60秒内一次性授权码")
    @NotBlank
    @Size(max = 128)
    private String code;

    @Schema(description = "签发时绑定的精确回调地址")
    @NotBlank
    @Size(max = 500)
    private String redirectUri;

    @Schema(description = "PKCE原始随机值，仅发起后端保存")
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._~-]{43,128}")
    private String codeVerifier;
}
