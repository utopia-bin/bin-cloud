package cn.utopiabin.cloud.common.redis;

import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.common.utils.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 工具 —— 融合 RedisTemplate(基本操作) + Redisson(锁/高级结构)
 *
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisClient {

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final RedisConfig redisConfig;

    // ==================== Key 前缀 ====================

    private String key(String k) {
        var prefix = redisConfig.getKeyPrefix();
        return StrUtil.isNotBlank(prefix) ? prefix + ":" + k : k;
    }

    // ==================== 通用 Key 操作 ====================

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key(key));
    }

    public boolean delete(String key) {
        return redisTemplate.delete(key(key));
    }

    public long delete(Collection<String> keys) {
        var prefixed = keys.stream().map(this::key).toList();
        return redisTemplate.delete(prefixed);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key(key), timeout, unit);
    }

    public boolean expire(String key, Duration duration) {
        return Boolean.TRUE.equals(redisTemplate.expire(key(key), duration));
    }

    public long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key(key), unit);
    }

    public Long incr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key(key), delta);
    }

    public Long incr(String key) {
        return incr(key, 1);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // ==================== String 操作 ====================

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key(key), value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key(key), value, timeout, unit);
    }

    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key(key), value, duration);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key(key));
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key(key));
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return JsonUtil.convert(value, clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAndDelete(String key) {
        return (T) redisTemplate.opsForValue().getAndDelete(key(key));
    }

    /** 仅当值匹配时原子删除，适合一次性令牌或验证码消费。 */
    public boolean compareAndDelete(String key, Object expectedValue) {
        Long result = redisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                List.of(key(key)),
                expectedValue);
        return result != null && result > 0;
    }

    public boolean setIfAbsent(String key, Object value, Duration duration) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key(key), value, duration));
    }

    // ==================== Hash 操作 ====================

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key(key), field, value);
    }

    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key(key), map);
    }

    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key(key), field);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key(key));
    }

    public boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key(key), field);
    }

    public long hDelete(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key(key), (Object[]) fields);
    }

    // ==================== List 操作 ====================

    public long lPush(String key, Object... values) {
        var count = redisTemplate.opsForList().leftPushAll(key(key), values);
        return count != null ? count : 0;
    }

    public long rPush(String key, Object... values) {
        var count = redisTemplate.opsForList().rightPushAll(key(key), values);
        return count != null ? count : 0;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) (List<?>) redisTemplate.opsForList().range(key(key), start, end);
    }

    @SuppressWarnings("unchecked")
    public <T> T lPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key(key));
    }

    // ==================== Set 操作 ====================

    public long sAdd(String key, Object... values) {
        var count = redisTemplate.opsForSet().add(key(key), values);
        return count != null ? count : 0;
    }

    public Set<Object> sMembers(String key) {
        var set = redisTemplate.opsForSet().members(key(key));
        return set != null ? set : Set.of();
    }

    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key(key), value));
    }

    // ==================== 分布式锁 (Redisson) ====================

    public RLock getLock(String lockKey) {
        return redissonClient.getLock(key(lockKey));
    }

    /**
     * 尝试获取锁并执行,自动释放
     */
    public <T> T lockAndRun(String lockKey, long waitSec, long leaseSec, Supplier<T> action) {
        var lock = getLock(lockKey);
        try {
            if (lock.tryLock(waitSec, leaseSec, TimeUnit.SECONDS)) {
                try {
                    return action.get();
                } finally {
                    unlock(lock);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public void lockAndRun(String lockKey, long waitSec, long leaseSec, Runnable action) {
        var lock = getLock(lockKey);
        try {
            if (lock.tryLock(waitSec, leaseSec, TimeUnit.SECONDS)) {
                try {
                    action.run();
                } finally {
                    unlock(lock);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void unlock(RLock lock) {
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    // ==================== 原始实例 ====================

    public RedisTemplate<String, Object> template() {
        return redisTemplate;
    }

    public RedissonClient redisson() {
        return redissonClient;
    }
}
