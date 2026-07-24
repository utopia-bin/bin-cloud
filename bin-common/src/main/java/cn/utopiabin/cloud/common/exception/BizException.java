package cn.utopiabin.cloud.common.exception;

import lombok.Getter;

/**
 * 全局基础业务异常
 *
 * <pre>{@code
 *   throw new BizException("用户名已存在");
 *   throw new BizException(1001, "余额不足");
 *   throw new BizException(1001, "余额不足", e);
 * }</pre>
 *
 * @since 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    /** 默认失败状态码 */
    public static final int FAIL_CODE = 500;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = FAIL_CODE;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
