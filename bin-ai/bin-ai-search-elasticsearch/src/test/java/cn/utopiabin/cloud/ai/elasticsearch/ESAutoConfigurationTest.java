package cn.utopiabin.cloud.ai.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ESAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ESAutoConfiguration.class));

    @Test
    void isDisabledWithoutApiUrl() {
        this.contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(ESClientFactory.class));
    }

    @Test
    void bindsConfigurationAndRegistersFactory() {
        this.contextRunner
                .withPropertyValues(
                        "es.api-url=https://localhost:9200",
                        "es.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(ESClientFactory.class);
                    ESConfig config = context.getBean(ESConfig.class);
                    assertThat(config.getApiUrl()).isEqualTo("https://localhost:9200");
                    assertThat(config.getApiKey()).isEqualTo("test-key");
                });
    }
}
