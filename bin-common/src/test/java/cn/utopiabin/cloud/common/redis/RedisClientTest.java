package cn.utopiabin.cloud.common.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisClientTest {

    @Test
    void shouldApplyPrefixAndHandleNullableTemplateResults() {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        RedisConfig redisConfig = mock(RedisConfig.class);
        when(redisConfig.getKeyPrefix()).thenReturn("app");
        when(redisTemplate.hasKey("app:user:1")).thenReturn(null);
        when(redisTemplate.delete("app:user:1")).thenReturn(null);
        when(redisTemplate.getExpire("app:user:1", TimeUnit.SECONDS)).thenReturn(null);
        when(redisTemplate.keys("app:user:*")).thenReturn(null);

        RedisClient client = new RedisClient(redisTemplate, mock(RedissonClient.class), redisConfig);

        assertFalse(client.hasKey("user:1"));
        assertFalse(client.delete("user:1"));
        assertEquals(-2, client.getExpire("user:1", TimeUnit.SECONDS));
        assertEquals(Set.of(), client.keys("user:*"));
        assertEquals(0, client.delete(List.of()));
        verify(redisTemplate, never()).delete(List.of());
    }

    @Test
    void shouldOnlyUnlockLockHeldByCurrentThread() throws InterruptedException {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        RedissonClient redissonClient = mock(RedissonClient.class);
        RedisConfig redisConfig = mock(RedisConfig.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("job")).thenReturn(lock);
        when(lock.tryLock(1, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        RedisClient client = new RedisClient(redisTemplate, redissonClient, redisConfig);
        String result = client.lockAndRun("job", 1, 5, () -> "done");

        assertEquals("done", result);
        verify(lock).isHeldByCurrentThread();
        verify(lock, never()).isLocked();
        verify(lock).unlock();
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }
}
