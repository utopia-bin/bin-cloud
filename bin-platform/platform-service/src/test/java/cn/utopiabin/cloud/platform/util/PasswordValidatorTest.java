package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {
    private final LoginSecurityProperties properties = new LoginSecurityProperties();
    private final PasswordValidator validator = new PasswordValidator(properties);

    @Test
    void policyReflectsRuntimeConfiguration() {
        properties.setPasswordMinLength(12);
        properties.setPasswordRequireSpecial(true);
        assertEquals(12, validator.policy().getMinLength());
        assertTrue(validator.policy().isRequireSpecial());
        assertThrows(BizException.class, () -> validator.validate("Strong123"));
        assertThrows(BizException.class, () -> validator.validate("Strong123456"));
        assertDoesNotThrow(() -> validator.validate("Strong123456!"));
    }

    @Test
    void rejectsWeakAndOverlongPasswordsBeforeEncoding() {
        for (String password : new String[]{"Aa1", "abcdefgh1", "ABCDEFGH1", "Abcdefghi",
                "Aa1" + "x".repeat(62), "Aa1" + "中".repeat(24)}) {
            assertThrows(BizException.class, () -> validator.validate(password));
        }
        assertThrows(BizException.class, () -> validator.validate(null));
        assertDoesNotThrow(() -> validator.validate("Aa1" + "中".repeat(23)));
        assertDoesNotThrow(() -> validator.validate("Strong123"));
    }
}
