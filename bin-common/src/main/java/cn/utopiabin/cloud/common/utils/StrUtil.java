package cn.utopiabin.cloud.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 字符串工具类 —— 融合 {@link StringUtils}
 *
 * @since 1.0.0
 */
public final class StrUtil {

    private static final Pattern UNDERLINE_TO_CAMEL = Pattern.compile("_(.)");
    private static final Pattern CAMEL_TO_UNDERLINE = Pattern.compile("([A-Z])");
    private static final Pattern BLANK_PATTERN = Pattern.compile("\\s+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private StrUtil() {
    }

    // ==================== 委托 StringUtils 常用方法 ====================

    // --- 空值判断 ---
    public static boolean isEmpty(CharSequence cs)               { return StringUtils.isEmpty(cs); }
    public static boolean isNotEmpty(CharSequence cs)             { return StringUtils.isNotEmpty(cs); }
    public static boolean isBlank(CharSequence cs)                { return StringUtils.isBlank(cs); }
    public static boolean isNotBlank(CharSequence cs)             { return StringUtils.isNotBlank(cs); }
    public static boolean isAnyEmpty(CharSequence... css)         { return StringUtils.isAnyEmpty(css); }
    public static boolean isAnyBlank(CharSequence... css)         { return StringUtils.isAnyBlank(css); }
    public static boolean isNoneBlank(CharSequence... css)        { return StringUtils.isNoneBlank(css); }

    // --- trim ---
    public static String trim(String str)                        { return StringUtils.trim(str); }
    public static String trimToNull(String str)                  { return StringUtils.trimToNull(str); }
    public static String trimToEmpty(String str)                 { return StringUtils.trimToEmpty(str); }

    // --- 相等 ---
    public static boolean equals(CharSequence a, CharSequence b) { return StringUtils.equals(a, b); }
    public static boolean equalsIgnoreCase(CharSequence a, CharSequence b) { return StringUtils.equalsIgnoreCase(a, b); }

    // --- 查找与包含 ---
    public static boolean contains(CharSequence seq, CharSequence search) { return StringUtils.contains(seq, search); }
    public static boolean containsIgnoreCase(CharSequence str, CharSequence search) { return StringUtils.containsIgnoreCase(str, search); }
    public static boolean containsAny(CharSequence cs, CharSequence... search) { return StringUtils.containsAny(cs, search); }

    // --- 子串 ---
    public static String substring(String str, int start)                        { return StringUtils.substring(str, start); }
    public static String substring(String str, int start, int end)               { return StringUtils.substring(str, start, end); }
    public static String left(String str, int len)                               { return StringUtils.left(str, len); }
    public static String right(String str, int len)                              { return StringUtils.right(str, len); }
    public static String substringBefore(String str, String separator)           { return StringUtils.substringBefore(str, separator); }
    public static String substringAfter(String str, String separator)            { return StringUtils.substringAfter(str, separator); }

    // --- 大小写 ---
    public static String upperCase(String str)                                   { return StringUtils.upperCase(str); }
    public static String lowerCase(String str)                                   { return StringUtils.lowerCase(str); }
    public static String capitalize(String str)                                  { return StringUtils.capitalize(str); }
    public static String uncapitalize(String str)                                { return StringUtils.uncapitalize(str); }

    // --- 分割与连接 ---
    public static String[] split(String str)                                     { return StringUtils.split(str); }
    public static String[] split(String str, String separator)                   { return StringUtils.split(str, separator); }
    public static String join(Object[] array, String separator)                  { return StringUtils.join(array, separator); }
    public static String join(Iterable<?> iterable, String separator)            { return StringUtils.join(iterable, separator); }

    // --- 替换 ---
    public static String replace(String text, String search, String replacement) { return StringUtils.replace(text, search, replacement); }
    public static String replaceOnce(String text, String search, String replacement) { return StringUtils.replaceOnce(text, search, replacement); }
    public static String replaceChars(String str, char search, char replace)     { return StringUtils.replaceChars(str, search, replace); }

    // --- 判断 ---
    public static boolean isNumeric(CharSequence cs)                             { return StringUtils.isNumeric(cs); }
    public static boolean isAlpha(CharSequence cs)                                { return StringUtils.isAlpha(cs); }
    public static boolean isAlphanumeric(CharSequence cs)                         { return StringUtils.isAlphanumeric(cs); }

    // --- 默认值 ---
    public static String defaultString(String str)                               { return StringUtils.defaultString(str); }
    public static String defaultIfEmpty(String str, String defaultStr)           { return StringUtils.defaultIfEmpty(str, defaultStr); }
    public static String defaultIfBlank(String str, String defaultStr)           { return StringUtils.defaultIfBlank(str, defaultStr); }

    // --- 缩写 ---
    public static String abbreviate(String str, int maxWidth)                    { return StringUtils.abbreviate(str, maxWidth); }
    public static String abbreviate(String str, int offset, int maxWidth)        { return StringUtils.abbreviate(str, offset, maxWidth); }

    // --- 其他 ---
    public static int length(CharSequence cs)                                    { return StringUtils.length(cs); }
    public static int countMatches(CharSequence str, CharSequence sub)           { return StringUtils.countMatches(str, sub); }
    public static String reverse(String str)                                     { return StringUtils.reverse(str); }
    public static String repeat(String str, int repeat)                          { return StringUtils.repeat(str, repeat); }
    public static String strip(String str)                                       { return StringUtils.strip(str); }
    public static String wrap(String str, String wrapWith)                       { return StringUtils.wrap(str, wrapWith); }

    // ==================== 本类扩展 ====================

    public static boolean hasText(CharSequence cs) {
        return isNotBlank(cs);
    }

    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    /** 按分隔符分割为 List,自动 trim 并过滤空串 */
    public static List<String> splitToList(String str, String separator) {
        if (isBlank(str)) {
            return List.of();
        }
        return java.util.Arrays.stream(str.split(Pattern.quote(separator)))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 按正则分割为 List,自动 trim 并过滤空串 */
    public static List<String> splitToList(String str, Pattern regex) {
        if (isBlank(str)) {
            return List.of();
        }
        return regex.splitAsStream(str)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 下划线转驼峰: user_name → userName */
    public static String toCamelCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        return UNDERLINE_TO_CAMEL.matcher(str.toLowerCase()).replaceAll(m -> m.group(1).toUpperCase());
    }

    /** 驼峰转下划线: userName → user_name */
    public static String toUnderscore(String str) {
        if (isBlank(str)) {
            return str;
        }
        return CAMEL_TO_UNDERLINE.matcher(str).replaceAll("_$1").toLowerCase();
    }

    /** 超长截断并追加 "..." */
    public static String truncateWithEllipsis(String str, int maxLength) {
        if (isBlank(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /** 移除所有空白字符 */
    public static String removeWhitespace(String str) {
        return isBlank(str) ? str : BLANK_PATTERN.matcher(str).replaceAll("");
    }

    /** 移除 HTML 标签 */
    public static String removeHtmlTag(String str) {
        return isBlank(str) ? str : HTML_TAG.matcher(str).replaceAll("");
    }

    /** 手机号脱敏: 13812341234 → 138****1234 */
    public static String maskPhone(String phone) {
        if (isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "*".repeat(4) + phone.substring(phone.length() - 4);
    }

    /** 邮箱脱敏: test@example.com → t***t@example.com */
    public static String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return email;
        }
        int idx = email.indexOf('@');
        String name = email.substring(0, idx);
        String domain = email.substring(idx);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    /** 安全 toString, null → "" */
    public static String str(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /** UTF-8 字节长度 */
    public static int byteLength(String str) {
        return str == null ? 0 : str.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
}
