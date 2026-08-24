package cn.utopiabin.cloud.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交
 * <p>
 * 标注在写操作方法上，同一用户对同一方法+相同参数在时间窗口内只允许提交一次。
 * 基于 Redis setIfAbsent 实现，窗口期内的重复请求直接拒绝。
 *
 * @since 1.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 防重时间窗口 (秒)，默认 3 秒
     */
    int interval() default 3;

    /**
     * 拒绝时的提示消息
     */
    String message() default "请勿重复提交，请稍后再试";
}
