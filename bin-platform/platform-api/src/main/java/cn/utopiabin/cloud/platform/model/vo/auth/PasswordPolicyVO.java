package cn.utopiabin.cloud.platform.model.vo.auth;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前生效的密码设置策略，不包含认证密钥")
public class PasswordPolicyVO extends JsonSerializable {
    @Schema(description = "最少字符数，由平台安全配置决定", example = "8")
    private int minLength;
    @Schema(description = "最多字符数", example = "64")
    private int maxLength = 64;
    @Schema(description = "UTF-8 编码后的最大字节数，避免超过 BCrypt 输入上限", example = "72")
    private int maxBytes = 72;
    @Schema(description = "是否必须包含 ASCII 大写字母 A-Z", example = "true")
    private boolean requireUppercase = true;
    @Schema(description = "是否必须包含 ASCII 小写字母 a-z", example = "true")
    private boolean requireLowercase = true;
    @Schema(description = "是否必须包含数字 0-9", example = "true")
    private boolean requireDigit = true;
    @Schema(description = "是否必须包含字母和数字以外的字符，由平台安全配置决定", example = "false")
    private boolean requireSpecial;
}
