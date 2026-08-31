package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.model.vo.auth.PasswordPolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 密码强度校验器
 * <p>
 * 校验规则由 {@link LoginSecurityProperties} 配置驱动:
 * <ul>
 *   <li>最小长度 (默认 8 位)</li>
 *   <li>必须同时包含大写字母、小写字母、数字</li>
 *   <li>可选: 必须包含特殊字符</li>
 * </ul>
 *
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class PasswordValidator {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    private final LoginSecurityProperties properties;

    public PasswordPolicyVO policy() {
        var policy = new PasswordPolicyVO();
        policy.setMinLength(properties.getPasswordMinLength());
        policy.setRequireSpecial(properties.isPasswordRequireSpecial());
        return policy;
    }

    /**
     * 校验密码强度，不通过则抛出业务异常
     *
     * @param password 明文密码
     * @throws BizException 密码强度不足
     */
    public void validate(String password) {
        var policy = policy();
        if (password != null && (password.length() > policy.getMaxLength()
                || password.getBytes(StandardCharsets.UTF_8).length > policy.getMaxBytes())) {
            throw new BizException(PlatformErrorCode.PASSWORD_WEAK.getCode(),
                    "密码不能超过 " + policy.getMaxLength() + " 个字符或 " + policy.getMaxBytes() + " 个 UTF-8 字节");
        }
        if (password == null || password.length() < properties.getPasswordMinLength()) {
            throw new BizException(PlatformErrorCode.PASSWORD_WEAK.getCode(),
                    "密码长度不能少于 " + properties.getPasswordMinLength() + " 位");
        }
        if (!UPPER.matcher(password).find()) {
            throw new BizException(PlatformErrorCode.PASSWORD_WEAK.getCode(),
                    "密码必须包含大写字母");
        }
        if (!LOWER.matcher(password).find()) {
            throw new BizException(PlatformErrorCode.PASSWORD_WEAK.getCode(),
                    "密码必须包含小写字母");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new BizException(PlatformErrorCode.PASSWORD_WEAK.getCode(),
                    "密码必须包含数字");
        }
        if (properties.isPasswordRequireSpecial() && !SPECIAL.matcher(password).find()) {
            throw new BizException(PlatformErrorCode.PASSWORD_WEAK.getCode(),
                    "密码必须包含特殊字符");
        }
    }
}
