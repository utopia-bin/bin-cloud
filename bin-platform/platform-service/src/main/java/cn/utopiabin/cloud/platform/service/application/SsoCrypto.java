package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.exception.BizException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class SsoCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    private SsoCrypto() {}

    public static String random() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }

    public static String hash(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    public static String challenge(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));
    }

    public static boolean equal(String a, String b) {
        return a != null
                && b != null
                && MessageDigest.isEqual(
                        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static String redirect(String value, String environment) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null
                    || uri.getRawQuery() != null
                    || !("https".equals(uri.getScheme())
                            || "DEV".equals(environment) && "http".equals(uri.getScheme()))
                    || !uri.normalize().equals(uri)) throw new IllegalArgumentException();
            return value;
        } catch (Exception e) {
            throw new BizException(400, "回调地址必须为完整精确地址，不能包含查询串或片段；本地HTTP请选择DEV");
        }
    }

    public static void navigation(String value, boolean emptyAllowed) {
        if (emptyAllowed && (value == null || value.isEmpty())) return;
        try {
            URI uri = URI.create(value);
            if (value.startsWith("/")
                    && !value.startsWith("//")
                    && uri.getAuthority() == null
                    && !value.contains("\\")) return;
            if (("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null
                    && uri.getUserInfo() == null) return;
        } catch (Exception ignored) {
        }
        throw new BizException(400, "入口或图标地址必须为本站路径或HTTP(S)地址");
    }
}
