package cn.utopiabin.cloud.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Swagger 文档聚合配置
 * <p>
 * 通过 Nacos 服务发现, 自动聚合所有下游服务的 OpenAPI 文档,
 * 在 Gateway 的 /swagger-ui.html 提供统一入口。
 * <p>
 * 路由前缀映射规则: serviceId → route prefix
 * <ul>
 *   <li>admin-api → /admin</li>
 *   <li>open-api → /open</li>
 *   <li>platform-api → /platform</li>
 *   <li>bin-thirdparty → /thirdparty</li>
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
            DiscoveryClient discoveryClient,
            SwaggerUiConfigProperties swaggerUiConfigProperties) {

        SwaggerUiConfigParameters configParameters = new SwaggerUiConfigParameters(swaggerUiConfigProperties);

        // 从 Nacos 发现所有下游服务, 根据路由前缀聚合 OpenAPI 文档
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = discoveryClient.getServices().stream()
                .filter(name -> !"bin-gateway".equals(name))
                .flatMap(serviceId -> discoveryClient.getInstances(serviceId).stream()
                        .map(instance -> {
                            String routePrefix = resolveRoutePrefix(serviceId);
                            return new AbstractSwaggerUiConfigProperties.SwaggerUrl(
                                    serviceId,
                                    "/" + routePrefix + "/v3/api-docs",
                                    serviceId
                            );
                        }))
                .collect(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(AbstractSwaggerUiConfigProperties.SwaggerUrl::getName))));

        configParameters.setUrls(urls);
        return configParameters;
    }

    /**
     * 根据 serviceId 推导路由前缀
     * <p>
     * 规则: 去除 "bin-" 前缀, 再去除 "-api" 后缀
     * <ul>
     *   <li>admin-api → admin</li>
     *   <li>bin-thirdparty → thirdparty</li>
     * </ul>
     */
    private String resolveRoutePrefix(String serviceId) {
        String prefix = serviceId;
        if (prefix.startsWith("bin-")) {
            prefix = prefix.substring("bin-".length());
        }
        if (prefix.endsWith("-api")) {
            prefix = prefix.substring(0, prefix.length() - "-api".length());
        }
        return prefix;
    }
}
