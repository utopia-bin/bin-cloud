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
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
@ConditionalOnClass({RedisTemplate.class, RedissonClient.class})
@EnableConfigurationProperties(RedisConfig.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({RedisTemplate.class, RedissonClient.class})
    @SuppressWarnings("unchecked")
    public RedisClient redisClient(RedisTemplate<? super String, Object> redisTemplate,
                                   RedissonClient redissonClient,
                                   RedisConfig redisConfig) {
        // 默认模板的 Key 类型为 Object，也能接受本客户端的 String Key；复用模板以保留序列化配置。
        return new RedisClient((RedisTemplate<String, Object>) redisTemplate, redissonClient, redisConfig);
    }
}
