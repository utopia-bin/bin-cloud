package cn.utopiabin.cloud.ai.elasticsearch;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

/**
 * ES 客户端工厂 (由 {@link ESAutoConfiguration} 自动注册)
 *
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class ESClientFactory {

    private final ESConfig esConfig;

    private volatile ElasticsearchClient client;
    private volatile ESConfig snapshot;

    private ElasticsearchClient buildClient() {
        if (client != null && esConfig.equals(snapshot)) {
            return client;
        }
        synchronized (this) {
            if (client != null && esConfig.equals(snapshot)) {
                return client;
            }
            var restClientBuilder = RestClient.builder(HttpHost.create(esConfig.getApiUrl()));
            if (!StrUtil.isBlank(esConfig.getApiKey())) {
                restClientBuilder.setDefaultHeaders(
                        new Header[]{new BasicHeader("Authorization", "ApiKey " + esConfig.getApiKey())});
            }
            var restClient = restClientBuilder.setHttpClientConfigCallback(http -> {
                        try {
                            var ctx = SSLContextBuilder.create()
                                    .loadTrustMaterial((chain, type) -> true).build();
                            http.setSSLContext(ctx);
                            http.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                            return http;
                        } catch (Exception e) {
                            throw new RuntimeException("SSL 初始化失败", e);
                        }
                    }).build();

            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            ElasticsearchClient oldClient = client;
            client = new ElasticsearchClient(transport);
            snapshot = esConfig.copyTo(ESConfig.class);
            closeQuietly(oldClient);
            log.info("ES 客户端初始化完成: {}", esConfig.getApiUrl());
        }
        return client;
    }

    public ElasticsearchClient get() {
        if (StrUtil.isBlank(esConfig.getApiUrl())) {
            throw new BizException("ES 未配置");
        }
        try {
            return buildClient();
        } catch (Exception e) {
            log.error("ES 客户端初始化失败: {}", e.getMessage(), e);
            throw new BizException("ES 客户端初始化失败");
        }
    }

    @PreDestroy
    public void close() {
        closeQuietly(client);
        client = null;
        snapshot = null;
    }

    private void closeQuietly(ElasticsearchClient target) {
        if (target == null) {
            return;
        }
        try {
            target.close();
        } catch (IOException e) {
            log.warn("ES 客户端关闭异常: {}", e.getMessage());
        }
    }
}
