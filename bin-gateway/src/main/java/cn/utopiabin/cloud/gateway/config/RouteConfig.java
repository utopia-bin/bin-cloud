package cn.utopiabin.cloud.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由配置 —— 程序式定义灰度路由规则
 * <p>
 * 静态路由通过 application.yml 的 spring.cloud.gateway.routes 声明,
 * 本类定义显式灰度路由 (请求头 X-Canary=true 时优先匹配)。
 * <p>
 * 灰度实例选择由 {@link cn.utopiabin.cloud.gateway.loadbalancer.CanaryServiceInstanceListSupplier}
 * 在 LoadBalancer 层完成, 下游服务需在 Nacos 注册时设置元数据 {@code canary=true}。
 *
 * @since 1.0.0
 */
@Configuration
public class RouteConfig {

    /**
     * 灰度高优先级路由: 当请求头 X-Canary=true 时路由到金丝雀实例
     */
    @Bean
    public RouteLocator canaryRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // admin-api 灰度路由
                .route("admin-api-canary", r -> r
                        .header("X-Canary", "true")
                        .and()
                        .path("/admin/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(c -> c
                                        .setName("adminApiCB")
                                        .setFallbackUri("forward:/fallback/admin")))
                        .uri("lb://admin-api"))
                // platform-api 灰度路由
                .route("platform-api-canary", r -> r
                        .header("X-Canary", "true")
                        .and()
                        .path("/platform/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(c -> c
                                        .setName("platformApiCB")
                                        .setFallbackUri("forward:/fallback/platform")))
                        .uri("lb://platform-api"))
                .build();
    }
}
