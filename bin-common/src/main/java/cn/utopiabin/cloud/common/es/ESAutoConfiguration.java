package cn.utopiabin.cloud.common.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ES 自动装配 —— 当 classpath 存在 ES 客户端且配置了 es.api-url 时自动注册
 *
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnClass(ElasticsearchClient.class)
@EnableConfigurationProperties(ESConfig.class)
@ConditionalOnProperty(prefix = "es", name = "api-url")
public class ESAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ESClientFactory esClientFactory(ESConfig esConfig) {
        return new ESClientFactory(esConfig);
    }
}
