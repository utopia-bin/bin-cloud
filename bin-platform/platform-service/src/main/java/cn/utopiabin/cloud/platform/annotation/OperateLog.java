package cn.utopiabin.cloud.platform.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志
 * <p>
 * 标注在写操作方法上，切面在方法执行后异步记录操作日志
 * (操作人、租户、模块、动作、参数摘要、结果、耗时、异常信息)。
 * <p>
 * 日志落库失败不影响主流程。
 *
 * @since 1.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {

    /**
     * 业务模块 (如 "用户管理")
     */
    String module();

    /**
     * 操作动作 (如 "新增用户")
     */
    String action();

    /**
     * 操作类型
     */
    OperateType type() default OperateType.OTHER;

    /**
     * 是否完全屏蔽参数 (不记录参数值，仅记录参数个数)
     * <p>
     * 适用于含裸敏感参数的方法 (如 resetPassword(userId, password)、logout(token))，
     * 此类参数非 DTO 字段形式，字段级脱敏正则无法覆盖。
     */
    boolean maskParams() default false;

    /**
     * 操作人 SpEL 表达式 (可选)
     * <p>
     * 用于用户上下文尚未建立的场景 (如登录前)，从方法参数中提取操作人。
     * 示例: {@code principalSpel = "#dto.username"}
     */
    String principalSpel() default "";
}
