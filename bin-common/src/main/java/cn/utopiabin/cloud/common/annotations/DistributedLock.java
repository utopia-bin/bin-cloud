package cn.utopiabin.cloud.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 * <p>
 * 标注在需要并发控制的方法上，方法执行期间持有 Redisson 分布式锁。
 * 锁 Key 支持 SpEL 表达式，可引用方法参数 (如 {@code "'user:' + #dto.userId"})。
 * <p>
 * 适用场景: 角色分配、密码重置等并发执行会产生竞态的写操作。
 *
 * @since 1.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁 Key (支持 SpEL，引用方法参数)
     */
    String key();

    /**
     * 获取锁的最长等待时间，超时抛出操作频繁异常
     */
    long waitTime() default 3;

    /**
     * 锁持有时间 (秒)。<b>-1 表示启用 Redisson 看门狗自动续期</b>，
     * 方法执行期间锁不会超时释放；显式设置正数则到点自动释放 (长任务慎用)。
     */
    long leaseTime() default -1;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
