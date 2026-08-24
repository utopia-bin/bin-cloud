package cn.utopiabin.cloud.common.aspect;

import cn.utopiabin.cloud.common.annotations.RepeatSubmit;
import cn.utopiabin.cloud.common.constant.CommonErrorCode;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;

/**
 * 防重复提交切面
 * <p>
 * 拦截 {@link RepeatSubmit} 注解方法，基于 Redis setIfAbsent 实现时间窗口内
 * "同一用户 + 同一方法 + 相同参数" 只允许提交一次。
 * <p>
 * 切面顺序 ({@code @Order(-1)}): 位于所有切面最外层，重复请求快速失败，
 * 不进入锁/事务等重量级逻辑。
 * <p>
 * Key 结构: {@code platform:repeat:{userId}:{method}:{SHA-256(参数)}
 * @since 1.0
 */
@Slf4j
@Order(-1)
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private static final String KEY_PREFIX = "bin-cloud:repeat:";

    private final RedisClient redisClient;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        String key = buildKey(joinPoint);
        boolean acquired = redisClient.setIfAbsent(
                key, "1", Duration.ofSeconds(repeatSubmit.interval()));
        if (!acquired) {
            log.warn("拦截重复提交: key={}", key);
            throw new BizException(CommonErrorCode.REPEAT_SUBMIT.getCode(), repeatSubmit.message());
        }
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 业务失败立即释放防重锁，允许用户修正后重试
            redisClient.delete(key);
            throw e;
        }
    }

    /**
     * 构建防重 Key: 用户 + 方法签名 + 参数摘要
     */
    private String buildKey(ProceedingJoinPoint joinPoint) {
        String userId = UserContextHolder.getUserId();
        String operator = (userId == null || userId.isBlank()) ? "anon" : userId;

        var signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        return KEY_PREFIX + operator + ":" + method + ":" + hashArgs(joinPoint.getArgs());
    }

    /**
     * 参数摘要 (SHA-256，防弱哈希碰撞；序列化失败时降级为 deepToString)
     */
    private String hashArgs(Object[] args) {
        String repr;
        try {
            String json = JsonUtil.toJson(args);
            repr = json != null ? json : Arrays.deepToString(args);
        } catch (Exception e) {
            repr = Arrays.deepToString(args);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(repr.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            // 摘要算法不可用时退化为 hashCode
            return Integer.toHexString(repr.hashCode());
        }
    }
}
