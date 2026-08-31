package cn.utopiabin.cloud.common.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

    @Test
    void shouldCreateClientAfterRedissonRegistersItsDefaultTemplate() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        RedissonAutoConfigurationV2.class,
                        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class))
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withPropertyValues("spring.data.redis.key-prefix=platform")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RedisClient.class);
                    assertThat(context.getBean(RedisClient.class).template())
                            .isSameAs(context.getBean("redisTemplate"));
                    assertThat(context.getBean(RedisConfig.class).getKeyPrefix()).isEqualTo("platform");
                });
    }

    @Test
    void shouldBackOffWhenRedisInfrastructureIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(RedisClient.class);
        });
    }

    @Test
    void shouldReuseUserDefinedStringKeyTemplateAndItsSerializers() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        RedissonAutoConfigurationV2.class,
                        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class))
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withUserConfiguration(StringKeyTemplateConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RedisClient.class);
                    RedisTemplate<String, Object> template = context.getBean(RedisClient.class).template();
                    assertThat(template).isSameAs(context.getBean("redisTemplate"));
                    assertThat(template.getKeySerializer()).isSameAs(StringRedisSerializer.UTF_8);
                });
    }

    @Test
    void shouldBackOffWhenRedissonIsNotOnClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(RedisClient.class);
                });
    }

    @Test
    void shouldBackOffWhenRedissonClientIsMissing() {
        contextRunner.withBean("redisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(RedisClient.class);
                });
    }

    @Test
    void shouldBackOffWhenRedisTemplateIsMissing() {
        contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(RedisClient.class);
                });
    }

    @Test
    void shouldPreserveUserDefinedClient() {
        RedisClient existingClient = mock(RedisClient.class);
        contextRunner
                .withBean("redisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class))
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean("customRedisClient", RedisClient.class, () -> existingClient)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RedisClient.class);
                    assertThat(context.getBean(RedisClient.class)).isSameAs(existingClient);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class StringKeyTemplateConfiguration {

        @Bean
        RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(StringRedisSerializer.UTF_8);
            return template;
        }
    }
}
