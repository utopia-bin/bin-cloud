package cn.utopiabin.cloud.ai.chat.tencent;

import cn.utopiabin.cloud.ai.core.chat.ChatCoreAutoConfiguration;
import cn.utopiabin.cloud.ai.core.chat.ChatModelRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class TencentChatAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TencentChatAutoConfiguration.class,
                    ChatCoreAutoConfiguration.class))
            .withUserConfiguration(ClientDependencies.class);

    @Test
    void isDisabledByDefault() {
        this.contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TencentChatModel.class);
            assertThat(context.getBean(ChatModelRegistry.class).providerIds()).isEmpty();
        });
    }

    @Test
    void registersTencentProviderWhenEnabled() {
        this.contextRunner
                .withPropertyValues(
                        "bin.ai.providers.tencent.enabled=true",
                        "bin.ai.providers.tencent.app-key=test-app-key",
                        "bin.ai.providers.tencent.defaults.model=test-model")
                .run(context -> {
                    assertThat(context).hasSingleBean(TencentChatModel.class);
                    assertThat(context.getBean(ChatModelRegistry.class).providerIds())
                            .containsExactly("tencent");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class ClientDependencies {

        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
