package cn.utopiabin.cloud.ai.milvus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MilvusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MilvusAutoConfiguration.class));

    @Test
    void isDisabledWithoutUri() {
        this.contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(MilvusClientFactory.class));
    }

    @Test
    void bindsConfigurationAndRegistersFactory() {
        this.contextRunner
                .withPropertyValues(
                        "milvus.uri=http://localhost:19530",
                        "milvus.database-name=image-search")
                .run(context -> {
                    assertThat(context).hasSingleBean(MilvusClientFactory.class);
                    MilvusConfig config = context.getBean(MilvusConfig.class);
                    assertThat(config.getUri()).isEqualTo("http://localhost:19530");
                    assertThat(config.getDatabaseName()).isEqualTo("image-search");
                });
    }
}
