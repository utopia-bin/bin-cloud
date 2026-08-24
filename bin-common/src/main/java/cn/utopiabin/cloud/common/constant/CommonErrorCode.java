package cn.utopiabin.cloud.common.constant;

import lombok.Getter;

/**
 * 通用错误码枚举
 * <p>
 * 统一管理用用业务错误码，替代 BizException 中的硬编码消息
 *
 * @since 1.0
 */
@Getter
public enum CommonErrorCode {

    REPEAT_SUBMIT(1009, "请勿重复提交，请稍后再试"),
    LOCK_CONFLICT(1010, "操作过于频繁，请稍后再试"),
    ;
    private final int code;
    private final String msg;

    CommonErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 构建异常消息
     */
    public String message(String detail) {
        return detail != null && !detail.isBlank() ? detail : this.msg;
    }
}
