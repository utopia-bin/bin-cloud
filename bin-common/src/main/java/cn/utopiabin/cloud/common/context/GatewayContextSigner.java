package cn.utopiabin.cloud.common.context;

import cn.utopiabin.cloud.common.utils.StrUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/** 网关用户上下文 HMAC 签名工具。 */
public final class GatewayContextSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private GatewayContextSigner() {
    }

    public static String sign(String secret, long timestamp, String userId, String username,
                              String tenantId, String roles) {
        if (StrUtil.isBlank(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "gateway.context.signing-secret must be configured with at least 32 bytes");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(canonical(timestamp, userId, username, tenantId, roles)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign gateway user context", e);
        }
    }

    public static boolean verify(String secret, String timestampValue, String signature,
                                 String userId, String username, String tenantId, String roles,
                                 Duration maxAge) {
        if (StrUtil.isBlank(secret) || StrUtil.isBlank(timestampValue) || StrUtil.isBlank(signature)
                || maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(timestampValue);
            long now = System.currentTimeMillis();
            long allowedSkew = maxAge.toMillis();
            if (timestamp < now - allowedSkew || timestamp > now + allowedSkew) {
                return false;
            }
            String expected = sign(secret, timestamp, userId, username, tenantId, roles);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    private static String canonical(long timestamp, String userId, String username,
                                    String tenantId, String roles) {
        return timestamp + "\n" + value(userId) + "\n" + value(username) + "\n"
                + value(tenantId) + "\n" + value(roles);
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
