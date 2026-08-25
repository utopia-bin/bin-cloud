package cn.utopiabin.cloud.common.rest;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 统一 RESTful 接口返回包装类
 *
 * @param <T> 响应数据类型
 * @since 1.0
 */
@Getter
@Setter
@ToString
@Schema(description = "统一 REST 接口响应")
public class RestResult<T> extends JsonSerializable {
    /**
     * 状态码
     */
    @Schema(description = "业务状态码，200 表示成功", example = "200")
    private int code;

    /**
     * 提示消息
     */
    @Schema(description = "请求处理结果说明", example = "操作成功")
    private String msg;

    /**
     * 响应数据
     */
    @Schema(description = "接口响应数据；无返回数据时为空")
    private T data;

    /**
     * 响应时间戳
     */
    @Schema(description = "生成响应时的 Unix 毫秒时间戳", example = "1787621400000")
    private long timestamp;

    private RestResult() {
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功 ====================

    public static <T> RestResult<T> ok() {
        var r = new RestResult<T>();
        r.code = HttpStatus.SUCCESS.getCode();
        r.msg = HttpStatus.SUCCESS.getMsg();
        return r;
    }

    public static <T> RestResult<T> ok(T data) {
        RestResult<T> r = RestResult.ok();
        r.data = data;
        return r;
    }

    public static <T> RestResult<T> ok(String msg, T data) {
        RestResult<T> r = ok(data);
        r.msg = msg;
        return r;
    }

    // ==================== 失败 ====================

    public static <T> RestResult<T> fail(HttpStatus status) {
        var r = new RestResult<T>();
        r.code = status.getCode();
        r.msg = status.getMsg();
        return r;
    }

    public static <T> RestResult<T> fail(int code, String msg) {
        var r = new RestResult<T>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> RestResult<T> fail(String msg) {
        return fail(HttpStatus.FAIL.getCode(), msg);
    }

    // ==================== 链式设置 ====================

    public RestResult<T> code(int code) {
        this.code = code;
        return this;
    }

    public RestResult<T> msg(String msg) {
        this.msg = msg;
        return this;
    }

    public RestResult<T> data(T data) {
        this.data = data;
        return this;
    }

    // ==================== 内部状态码枚举 ====================

    @Getter
    public enum HttpStatus {

        SUCCESS(200, "操作成功"),
        FAIL(500, "操作失败"),
        ;

        private final int code;
        private final String msg;

        HttpStatus(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }
    }
}
