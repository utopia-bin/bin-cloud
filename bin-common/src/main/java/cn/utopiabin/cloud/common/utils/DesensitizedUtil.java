package cn.utopiabin.cloud.common.utils;

/**
 * 脱敏工具类
 *
 * @since 1.0
 */
public final class DesensitizedUtil {

    private DesensitizedUtil() {
    }

    // ==================== 通用掩码 ====================

    /**
     * 替换指定区间内字符为 '*'
     */
    public static String normalMasked(String source, int startInclude, int endExclude) {
        return normalMasked(source, startInclude, endExclude, '*');
    }

    /**
     * 替换指定区间内字符为指定字符
     */
    public static String normalMasked(String source, int startInclude, int endExclude, char replacedChar) {
        if (StrUtil.isBlank(source) || startInclude < 0 || endExclude < 0) {
            return source;
        }
        var val = source.trim();
        var len = val.length();
        if (startInclude >= len) {
            return val;
        }
        var end = Math.min(endExclude, len);
        if (startInclude >= end) {
            return val;
        }
        return val.substring(0, startInclude)
                + String.valueOf(replacedChar).repeat(end - startInclude)
                + val.substring(end);
    }

    // ==================== 各类型脱敏实现 (public: 供枚举策略引用) ====================

    public static String maskChineseName(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim();
        return val.length() <= 2
                ? normalMasked(val, 1, val.length(), '*')
                : normalMasked(val, 1, val.length() - 1);
    }

    public static String maskIdCard(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim();
        var retain = val.length() == 18 ? 4 : 3;
        return normalMasked(val, 6, val.length() - retain);
    }

    public static String maskMobilePhone(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim();
        return normalMasked(val, 3, val.length() - 4);
    }

    public static String maskFixedPhone(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim();
        return normalMasked(val, 4, val.length() - 2);
    }

    public static String maskEmail(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim();
        var atIdx = val.indexOf('@');
        return atIdx <= 1 ? val : normalMasked(val, 1, atIdx);
    }

    public static String maskBankCard(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim().replace(" ", "");
        var len = val.length();
        if (len < 9) {
            return val;
        }
        var endLen = len % 4 == 0 ? 4 : len % 4;
        var midLen = len - 4 - endLen;
        var sb = new StringBuilder();
        sb.append(val, 0, 4);
        for (var i = 0; i < midLen; i++) {
            if (i % 4 == 0) {
                sb.append(' ');
            }
            sb.append('*');
        }
        sb.append(' ').append(val, len - endLen, len);
        return sb.toString();
    }

    public static String maskCarLicense(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        return normalMasked(source.trim(), 3, 6);
    }

    public static String maskAddress(String source, int start, int end) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        var val = source.trim();
        return normalMasked(val, val.length() - 8, val.length());
    }

    public static String maskCustom(String source, int startInclude, int endExclude) {
        if (StrUtil.isBlank(source)) {
            return source;
        }
        return normalMasked(source.trim(), startInclude, endExclude);
    }

    public static String maskUserId(String source, int start, int end) {
        return "0";
    }

    public static String maskPassword(String source, int start, int end) {
        return "*".repeat(6);
    }
}
