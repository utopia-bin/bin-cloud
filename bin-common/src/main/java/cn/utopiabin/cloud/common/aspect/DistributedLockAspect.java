package cn.utopiabin.cloud.common.aspect;

import cn.utopiabin.cloud.common.annotations.DistributedLock;
import cn.utopiabin.cloud.common.constant.CommonErrorCode;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * 分布式锁切面
 * <p>
 * 拦截 {@link DistributedLock} 注解方法，方法执行期间持有 Redisson 分布式锁。
 * 锁 Key 支持 SpEL 引用方法参数；获取锁超时抛出"操作频繁"业务异常。
 * <p>
 * 切面顺序 ({@code @Order(0)}): 位于事务拦截器之外 —— "锁包事务"，
 * 保证锁在事务提交后才释放，消除并发窗口期。
 *
 * @since 1.0
 */
@Slf4j
@Order(0)
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String KEY_PREFIX = "bin-cloud:lock:";

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private final RedisClient redisClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = KEY_PREFIX + resolveKey(joinPoint, distributedLock.key());
        var lock = redisClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(),
                    distributedLock.unit());
            if (!locked) {
                log.warn("获取分布式锁超时: key={}", lockKey);
                throw new BizException(CommonErrorCode.LOCK_CONFLICT.getCode(),
                        CommonErrorCode.LOCK_CONFLICT.getMsg());
            }
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.LOCK_CONFLICT.getCode(),
                    CommonErrorCode.LOCK_CONFLICT.getMsg());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.warn("释放分布式锁失败: key={}", lockKey, e);
                }
            }
        }
    }

    /**
     * 解析 SpEL 锁 Key，解析失败时降级为方法签名 + 用户
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var method = signature.getMethod();
            var context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, joinPoint.getArgs(),
                    new DefaultParameterNameDiscoverer());
            String value = SPEL_PARSER.parseExpression(keyExpression).getValue(context, String.class);
            if (value != null && !value.isBlank()) {
                return value;
            }
        } catch (Exception e) {
            log.warn("分布式锁 SpEL 解析失败, 降级为方法签名: expr={}, error={}", keyExpression, e.getMessage());
        }
        var signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName()
                + ":" + UserContextHolder.getUserId();
    }
}
