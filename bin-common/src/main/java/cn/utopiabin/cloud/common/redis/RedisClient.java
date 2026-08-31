package cn.utopiabin.cloud.common.redis;

import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.common.utils.StrUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.connection.RedisStringCommands.BitOperation;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 工具 —— 融合 RedisTemplate(基本操作) + Redisson(锁/高级结构)
 *
 * @since 1.0
 */
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
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(key)));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key(key)));
    }

    public long delete(Collection<String> keys) {
        if (keys.isEmpty()) {
            return 0;
        }
        var prefixed = keys.stream().map(this::key).toList();
        return redisTemplate.delete(prefixed);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key(key), timeout, unit);
    }

    public void expire(String key, Duration duration) {
        redisTemplate.expire(key(key), duration);
    }

    public long getExpire(String key, TimeUnit unit) {
        // Redis TTL 约定：-1 表示永久，-2 表示 key 不存在；流水线/事务的空结果按不存在处理。
        Long ttl = redisTemplate.getExpire(key(key), unit);
        return ttl == null ? -2L : ttl;
    }

    public Long incr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key(key), delta);
    }

    public Long incr(String key) {
        return incr(key, 1);
    }

    public Set<String> keys(String pattern) {
        Set<String> result = redisTemplate.keys(key(pattern));
        return result == null ? Set.of() : result;
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

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key(key));
    }

    public <T> T get(String key, Class<T> clazz) {
        return convertValue(get(key), clazz);
    }

    public Object getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key(key));
    }

    public <T> T getAndDelete(String key, Class<T> clazz) {
        return convertValue(getAndDelete(key), clazz);
    }

    /** 仅当值匹配时原子删除，适合一次性令牌或验证码消费。 */
    public boolean compareAndDelete(String key, Object expectedValue) {
        Long result = redisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                List.of(key(key)),
                expectedValue);
        return result > 0;
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

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key(key), field);
    }

    public <T> T hGet(String key, String field, Class<T> clazz) {
        return convertValue(hGet(key, field), clazz);
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

    public Long hIncrement(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key(key), field, delta);
    }

    public long hSize(String key) {
        return redisTemplate.opsForHash().size(key(key));
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

    public List<Object> lRange(String key, long start, long end) {
        List<Object> values = redisTemplate.opsForList().range(key(key), start, end);
        return values != null ? values : List.of();
    }

    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        return lRange(key, start, end).stream().map(value -> convertValue(value, clazz)).toList();
    }

    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key(key));
    }

    public <T> T lPop(String key, Class<T> clazz) {
        return convertValue(lPop(key), clazz);
    }

    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key(key));
    }

    public <T> T rPop(String key, Class<T> clazz) {
        return convertValue(rPop(key), clazz);
    }

    public Object lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key(key), index);
    }

    public <T> T lIndex(String key, long index, Class<T> clazz) {
        return convertValue(lIndex(key, index), clazz);
    }

    public long lRemove(String key, long count, Object value) {
        Long removed = redisTemplate.opsForList().remove(key(key), count, value);
        return removed != null ? removed : 0;
    }

    public long lSize(String key) {
        Long size = redisTemplate.opsForList().size(key(key));
        return size != null ? size : 0;
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

    public long sRemove(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(key(key), values);
        return count != null ? count : 0;
    }

    public Object sPop(String key) {
        return redisTemplate.opsForSet().pop(key(key));
    }

    public <T> T sPop(String key, Class<T> clazz) {
        return convertValue(sPop(key), clazz);
    }

    public long sSize(String key) {
        Long size = redisTemplate.opsForSet().size(key(key));
        return size != null ? size : 0;
    }

    // ==================== ZSet 操作 ====================

    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key(key), value, score));
    }

    public long zAdd(String key, Set<TypedTuple<Object>> tuples) {
        var count = redisTemplate.opsForZSet().add(key(key), tuples);
        return count != null ? count : 0;
    }

    public long zRemove(String key, Object... values) {
        var count = redisTemplate.opsForZSet().remove(key(key), values);
        return count != null ? count : 0;
    }

    public Double zIncrementScore(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key(key), value, delta);
    }

    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key(key), value);
    }

    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key(key), value);
    }

    public Long zReverseRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key(key), value);
    }

    public long zSize(String key) {
        var size = redisTemplate.opsForZSet().size(key(key));
        return size != null ? size : 0;
    }

    public long zCount(String key, double min, double max) {
        var count = redisTemplate.opsForZSet().count(key(key), min, max);
        return count != null ? count : 0;
    }

    public Set<Object> zRange(String key, long start, long end) {
        var values = redisTemplate.opsForZSet().range(key(key), start, end);
        return values != null ? values : Set.of();
    }

    public Set<Object> zReverseRange(String key, long start, long end) {
        var values = redisTemplate.opsForZSet().reverseRange(key(key), start, end);
        return values != null ? values : Set.of();
    }

    public Set<Object> zRangeByScore(String key, double min, double max) {
        var values = redisTemplate.opsForZSet().rangeByScore(key(key), min, max);
        return values != null ? values : Set.of();
    }

    public Set<Object> zReverseRangeByScore(String key, double min, double max) {
        var values = redisTemplate.opsForZSet().reverseRangeByScore(key(key), min, max);
        return values != null ? values : Set.of();
    }

    public Set<TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        var values = redisTemplate.opsForZSet().rangeWithScores(key(key), start, end);
        return values != null ? values : Set.of();
    }

    public Set<TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        var values = redisTemplate.opsForZSet().reverseRangeWithScores(key(key), start, end);
        return values != null ? values : Set.of();
    }

    public Set<TypedTuple<Object>> zRangeByScoreWithScores(String key, double min, double max) {
        var values = redisTemplate.opsForZSet().rangeByScoreWithScores(key(key), min, max);
        return values != null ? values : Set.of();
    }

    public Set<TypedTuple<Object>> zReverseRangeByScoreWithScores(String key, double min, double max) {
        var values = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key(key), min, max);
        return values != null ? values : Set.of();
    }

    public long zRemoveRange(String key, long start, long end) {
        var count = redisTemplate.opsForZSet().removeRange(key(key), start, end);
        return count != null ? count : 0;
    }

    public long zRemoveRangeByScore(String key, double min, double max) {
        var count = redisTemplate.opsForZSet().removeRangeByScore(key(key), min, max);
        return count != null ? count : 0;
    }

    // ==================== Bitmap 操作 ====================

    public boolean setBit(String key, long offset, boolean value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setBit(key(key), offset, value));
    }

    public boolean getBit(String key, long offset) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key(key), offset));
    }

    public long bitCount(String key) {
        byte[] rawKey = serializeKey(key);
        Long count = redisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.stringCommands().bitCount(rawKey));
        return count != null ? count : 0;
    }

    public long bitCount(String key, long start, long end) {
        byte[] rawKey = serializeKey(key);
        Long count = redisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.stringCommands().bitCount(rawKey, start, end));
        return count != null ? count : 0;
    }

    public List<Long> bitField(String key, BitFieldSubCommands commands) {
        var values = redisTemplate.opsForValue().bitField(key(key), commands);
        return values != null ? values : List.of();
    }

    public long bitAnd(String destinationKey, String... sourceKeys) {
        return bitOp(BitOperation.AND, destinationKey, sourceKeys);
    }

    public long bitOr(String destinationKey, String... sourceKeys) {
        return bitOp(BitOperation.OR, destinationKey, sourceKeys);
    }

    public long bitXor(String destinationKey, String... sourceKeys) {
        return bitOp(BitOperation.XOR, destinationKey, sourceKeys);
    }

    public long bitNot(String destinationKey, String sourceKey) {
        return bitOp(BitOperation.NOT, destinationKey, sourceKey);
    }

    private long bitOp(BitOperation operation, String destinationKey, String... sourceKeys) {
        byte[] rawDestinationKey = serializeKey(destinationKey);
        byte[][] rawSourceKeys = Arrays.stream(sourceKeys)
                .map(this::serializeKey)
                .toArray(byte[][]::new);
        Long length = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitOp(operation, rawDestinationKey, rawSourceKeys));
        return length != null ? length : 0;
    }

    private byte[] serializeKey(String key) {
        return Objects.requireNonNull(
                redisTemplate.getStringSerializer().serialize(key(key)),
                "Redis key serializer returned null");
    }

    private <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return JsonUtil.convert(value, clazz);
    }

    // ==================== HyperLogLog 操作 ====================

    public long pfAdd(String key, Object... values) {
        return redisTemplate.opsForHyperLogLog().add(key(key), values);
    }

    public long pfCount(String... keys) {
        String[] prefixedKeys = Arrays.stream(keys).map(this::key).toArray(String[]::new);
        return redisTemplate.opsForHyperLogLog().size(prefixedKeys);
    }

    public long pfMerge(String destinationKey, String... sourceKeys) {
        String[] prefixedSourceKeys = Arrays.stream(sourceKeys).map(this::key).toArray(String[]::new);
        return redisTemplate.opsForHyperLogLog().union(key(destinationKey), prefixedSourceKeys);
    }

    // ==================== Geo 操作 ====================

    public long geoAdd(String key, Point point, Object member) {
        var count = redisTemplate.opsForGeo().add(key(key), point, member);
        return count != null ? count : 0;
    }

    public long geoAdd(String key, Map<Object, Point> locations) {
        var count = redisTemplate.opsForGeo().add(key(key), locations);
        return count != null ? count : 0;
    }

    public Distance geoDistance(String key, Object member1, Object member2) {
        return redisTemplate.opsForGeo().distance(key(key), member1, member2);
    }

    public Distance geoDistance(String key, Object member1, Object member2, Metric metric) {
        return redisTemplate.opsForGeo().distance(key(key), member1, member2, metric);
    }

    public List<Point> geoPosition(String key, Object... members) {
        var positions = redisTemplate.opsForGeo().position(key(key), members);
        return positions != null ? positions : List.of();
    }

    public List<String> geoHash(String key, Object... members) {
        var hashes = redisTemplate.opsForGeo().hash(key(key), members);
        return hashes != null ? hashes : List.of();
    }

    public GeoResults<GeoLocation<Object>> geoRadius(String key, Circle circle) {
        return redisTemplate.opsForGeo().radius(key(key), circle);
    }

    public GeoResults<GeoLocation<Object>> geoRadius(String key, Circle circle, GeoRadiusCommandArgs args) {
        return redisTemplate.opsForGeo().radius(key(key), circle, args);
    }

    public GeoResults<GeoLocation<Object>> geoRadius(String key, Object member, Distance distance) {
        return redisTemplate.opsForGeo().radius(key(key), member, distance);
    }

    public GeoResults<GeoLocation<Object>> geoRadius(
            String key, Object member, Distance distance, GeoRadiusCommandArgs args) {
        return redisTemplate.opsForGeo().radius(key(key), member, distance, args);
    }

    public long geoRemove(String key, Object... members) {
        var count = redisTemplate.opsForGeo().remove(key(key), members);
        return count != null ? count : 0;
    }

    // ==================== Stream 操作 ====================

    public RecordId xAdd(String key, Map<?, ?> content) {
        return redisTemplate.opsForStream().add(key(key), content);
    }

    public List<MapRecord<String, Object, Object>> xRange(String key, Range<String> range) {
        var records = redisTemplate.opsForStream().range(key(key), range);
        return records != null ? records : List.of();
    }

    public List<MapRecord<String, Object, Object>> xReverseRange(String key, Range<String> range) {
        var records = redisTemplate.opsForStream().reverseRange(key(key), range);
        return records != null ? records : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<MapRecord<String, Object, Object>> xRead(
            String key, ReadOffset offset, StreamReadOptions options) {
        var records = redisTemplate.opsForStream().read(options, StreamOffset.create(key(key), offset));
        return records != null ? records : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<MapRecord<String, Object, Object>> xReadGroup(
            String key, String group, String consumer, ReadOffset offset, StreamReadOptions options) {
        var records = redisTemplate.opsForStream().read(
                Consumer.from(group, consumer), options, StreamOffset.create(key(key), offset));
        return records != null ? records : List.of();
    }

    public long xAck(String key, String group, String... recordIds) {
        var count = redisTemplate.opsForStream().acknowledge(key(key), group, recordIds);
        return count != null ? count : 0;
    }

    public long xDelete(String key, String... recordIds) {
        var count = redisTemplate.opsForStream().delete(key(key), recordIds);
        return count != null ? count : 0;
    }

    public long xTrim(String key, long maxLength, boolean approximate) {
        var count = redisTemplate.opsForStream().trim(key(key), maxLength, approximate);
        return count != null ? count : 0;
    }

    public long xSize(String key) {
        var size = redisTemplate.opsForStream().size(key(key));
        return size != null ? size : 0;
    }

    public String xCreateGroup(String key, ReadOffset offset, String group) {
        return redisTemplate.opsForStream().createGroup(key(key), offset, group);
    }

    public boolean xDestroyGroup(String key, String group) {
        return Boolean.TRUE.equals(redisTemplate.opsForStream().destroyGroup(key(key), group));
    }

    public boolean xDeleteConsumer(String key, String group, String consumer) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForStream().deleteConsumer(key(key), Consumer.from(group, consumer)));
    }

    public PendingMessagesSummary xPending(String key, String group) {
        return redisTemplate.opsForStream().pending(key(key), group);
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
        if (lock.isHeldByCurrentThread()) {
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
