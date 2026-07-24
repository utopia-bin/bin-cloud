package cn.utopiabin.cloud.common.model.enums;

import cn.utopiabin.cloud.common.utils.DesensitizedUtil;

/**
 * 脱敏类型枚举 —— 枚举自带脱敏策略,新增类型只需加一行
 *
 * @since 1.0
 */
public enum DesensitizeType {

    /** 自定义区间 */
    CUSTOM(DesensitizedUtil::maskCustom),

    /** 中文姓名 */
    CHINESE_NAME(DesensitizedUtil::maskChineseName),

    /** 身份证号 */
    ID_CARD(DesensitizedUtil::maskIdCard),

    /** 手机号 */
    MOBILE_PHONE(DesensitizedUtil::maskMobilePhone),

    /** 座机号 */
    FIXED_PHONE(DesensitizedUtil::maskFixedPhone),

    /** 邮箱 */
    EMAIL(DesensitizedUtil::maskEmail),

    /** 银行卡 */
    BANK_CARD(DesensitizedUtil::maskBankCard),

    /** 车牌 */
    CAR_LICENSE(DesensitizedUtil::maskCarLicense),

    /** 地址 */
    ADDRESS(DesensitizedUtil::maskAddress),

    /** 用户 ID */
    USER_ID(DesensitizedUtil::maskUserId),

    /** 密码 */
    PASSWORD(DesensitizedUtil::maskPassword);

    private final TriMasker masker;

    DesensitizeType(TriMasker masker) {
        this.masker = masker;
    }

    /**
     * 执行脱敏
     */
    public String mask(String source, int startInclude, int endExclude) {
        return masker.apply(source, Math.max(0, startInclude), Math.max(0, endExclude));
    }

    @FunctionalInterface
    interface TriMasker {
        String apply(String source, int startInclude, int endExclude);
    }
}
