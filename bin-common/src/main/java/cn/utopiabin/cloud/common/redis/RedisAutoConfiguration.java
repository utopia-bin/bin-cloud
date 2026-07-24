package cn.utopiabin.cloud.common.redis;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis 自动装配 —— classpath 同时存在 RedisTemplate 和 RedissonClient 时自动注册
 *
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnClass({RedisTemplate.class, RedissonClient.class})
@EnableConfigurationProperties(RedisConfig.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({RedisTemplate.class, RedissonClient.class})
    public RedisClient redisClient(RedisTemplate<String, Object> redisTemplate,
                                   RedissonClient redissonClient,
                                   RedisConfig redisConfig) {
        return new RedisClient(redisTemplate, redissonClient, redisConfig);
    }
}
