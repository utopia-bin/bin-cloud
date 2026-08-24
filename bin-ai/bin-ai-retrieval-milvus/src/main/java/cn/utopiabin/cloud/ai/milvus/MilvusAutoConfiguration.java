package cn.utopiabin.cloud.ai.milvus;

import io.milvus.v2.client.MilvusClientV2;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Activates Milvus infrastructure when {@code milvus.uri} is configured.
 */
@AutoConfiguration
@ConditionalOnClass(MilvusClientV2.class)
@EnableConfigurationProperties(MilvusConfig.class)
@ConditionalOnProperty(prefix = MilvusConfig.PREFIX, name = "uri")
public class MilvusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MilvusClientFactory milvusClientFactory(MilvusConfig milvusConfig) {
        return new MilvusClientFactory(milvusConfig);
    }
}
