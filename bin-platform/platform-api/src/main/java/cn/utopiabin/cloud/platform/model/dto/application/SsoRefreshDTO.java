package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用后端刷新令牌轮换")
public class SsoRefreshDTO extends JsonSerializable {
    @Schema(description = "登记的serviceId")
    @NotBlank
    private String clientId;

    @Schema(description = "客户端凭证")
    @NotBlank
    @Size(max = 200)
    private String clientSecret;

    @Schema(description = "上一次兑换或刷新得到的随机令牌，成功后失效")
    @NotBlank
    @Size(max = 128)
    private String refreshToken;
}
