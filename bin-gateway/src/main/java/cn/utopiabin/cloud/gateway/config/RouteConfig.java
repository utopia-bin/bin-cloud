package cn.utopiabin.cloud.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关基线业务路由。
 * <p>
 * 灰度选择在 GlobalFilter 和 LoadBalancer 实例过滤阶段完成，因此路由谓词不直接
 * 信任外部 {@code X-Canary} 请求头。Nacos 可继续提供额外路由，但三个核心服务在
 * 配置中心暂时不可用时仍具有确定的基础路由。
 *
 * @since 1.0.0
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator businessRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("admin-api", route -> route.path("/admin/**")
                        .filters(filters -> filters.stripPrefix(1)
                                .circuitBreaker(config -> config.setName("adminApiCB")
                                        .setFallbackUri("forward:/fallback/admin")))
                        .uri("lb://admin-api"))
                .route("open-api", route -> route.path("/open/**")
                        .filters(filters -> filters.stripPrefix(1)
                                .circuitBreaker(config -> config.setName("openApiCB")
                                        .setFallbackUri("forward:/fallback/open")))
                        .uri("lb://open-api"))
                .route("platform-service", route -> route.path("/platform/**")
                        .filters(filters -> filters.stripPrefix(1)
                                .circuitBreaker(config -> config.setName("platformApiCB")
                                        .setFallbackUri("forward:/fallback/platform")))
                        .uri("lb://platform-service"))
                .build();
    }
}
