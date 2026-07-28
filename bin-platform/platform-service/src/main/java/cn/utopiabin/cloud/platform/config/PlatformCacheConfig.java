package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.platform.constant.CacheConstants;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache + Redis 配置
 * <p>
 * 启用声明式缓存，配置 JSON 序列化与分缓存 TTL:
 * <ul>
 *   <li>menu:tree — TTL 2 小时 (菜单变更少)</li>
 *   <li>user:perm — TTL 30 分钟 (权限缓存)</li>
 *   <li>默认 — TTL 30 分钟</li>
 * </ul>
 *
 * @since 1.0
 */
@Configuration
@EnableCaching
public class PlatformCacheConfig {

    /** 默认缓存 TTL */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /** 菜单树缓存 TTL */
    private static final Duration MENU_TREE_TTL = Duration.ofHours(2);

    /** 用户权限缓存 TTL */
    private static final Duration USER_PERM_TTL = Duration.ofMinutes(30);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(DEFAULT_TTL);

        Map<String, RedisCacheConfiguration> initialCacheConfigs = new HashMap<>();
        initialCacheConfigs.put(CacheConstants.MENU_TREE, defaultConfig.entryTtl(MENU_TREE_TTL));
        initialCacheConfigs.put(CacheConstants.USER_PERM, defaultConfig.entryTtl(USER_PERM_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(initialCacheConfigs)
                .build();
    }
}
