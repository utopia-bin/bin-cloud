package cn.utopiabin.cloud.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Swagger 文档聚合配置
 * <p>
 * 通过网关稳定路由聚合下游服务的 OpenAPI 文档，在 Gateway 的
 * {@code /swagger-ui.html} 提供统一入口。
 * <p>
 * 文档地址使用稳定的网关前缀，不依赖应用启动瞬间的服务发现快照；
 * 下游实例上下线由对应的 {@code lb://} 路由和负载均衡器处理。
 * <ul>
 *   <li>admin-api → /admin</li>
 *   <li>open-api → /open</li>
 *   <li>platform-service → /platform</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * 聚合下游服务的 Swagger 资源
     * <p>
     * SpringDoc 2.7.0+ 中 {@link SwaggerUiConfigParameters} 必须通过
     * {@code SwaggerUiConfigProperties} 构造, 无参构造已移除。
     */
    @Bean
    public SwaggerUiConfigParameters swaggerUiConfigParameters(
            SwaggerUiConfigProperties swaggerUiConfigProperties) {

        SwaggerUiConfigParameters configParameters = new SwaggerUiConfigParameters(swaggerUiConfigProperties);
        configParameters.setUrls(swaggerUrls());
        return configParameters;
    }

    Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrls() {
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new LinkedHashSet<>();
        urls.add(swaggerUrl("admin-api", "/admin/v3/api-docs"));
        urls.add(swaggerUrl("open-api", "/open/v3/api-docs"));
        urls.add(swaggerUrl("platform-service", "/platform/v3/api-docs"));
        return urls;
    }

    private AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl(String serviceId, String url) {
        return new AbstractSwaggerUiConfigProperties.SwaggerUrl(serviceId, url, serviceId);
    }
}
