package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.gateway.config.GatewayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 灰度金丝雀全局过滤器
 * <p>
 * 支持两种灰度策略:
 * 1. 请求头匹配: 携带 X-Canary=true 或 X-Version=<version> 的请求路由到灰度实例
 * 2. 比例灰度: 按配置比例 (canaryRatio) 将流量随机分配到灰度实例
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanaryFilter implements GlobalFilter, Ordered {

    private final GatewayConfig gatewayConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayConfig.isCanaryEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String canaryHeader = request.getHeaders().getFirst(CommonConstants.HEADER_CANARY);
        String versionHeader = request.getHeaders().getFirst(CommonConstants.HEADER_VERSION);

        // 策略 1: 请求头显式指定金丝雀
        boolean isCanaryRequest = isCanaryByHeader(canaryHeader, versionHeader);

        // 策略 2: 按比例灰度
        if (!isCanaryRequest && gatewayConfig.getCanaryRatio() > 0) {
            isCanaryRequest = isCanaryByRatio();
        }

        if (isCanaryRequest) {
            log.debug("灰度请求命中: path={}, canaryHeader={}, versionHeader={}",
                    request.getURI().getPath(), canaryHeader, versionHeader);
            // 注入灰度标记, 供后续 LoadBalancer 使用
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(CommonConstants.HEADER_CANARY, "true")
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        return chain.filter(exchange);
    }

    /**
     * 通过请求头判断是否为金丝雀请求
     */
    private boolean isCanaryByHeader(String canaryHeader, String versionHeader) {
        return "true".equalsIgnoreCase(canaryHeader)
                || (versionHeader != null && versionHeader.contains("canary"));
    }

    /**
     * 按比例随机判断是否进入金丝雀通道
     */
    private boolean isCanaryByRatio() {
        return ThreadLocalRandom.current().nextDouble() < gatewayConfig.getCanaryRatio();
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
