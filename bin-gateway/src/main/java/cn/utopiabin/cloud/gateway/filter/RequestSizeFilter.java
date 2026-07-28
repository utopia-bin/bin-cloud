package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.gateway.config.GatewayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 请求体大小限制全局过滤器
 * <p>
 * 在请求进入路由前检查 Content-Length, 超过阈值直接返回 413,
 * 防止超大请求体耗尽网关内存资源。
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestSizeFilter implements GlobalFilter, Ordered {

    private final GatewayConfig gatewayConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long contentLength = request.getHeaders().getContentLength();
        long maxSize = gatewayConfig.getMaxRequestSize();

        if (contentLength > maxSize) {
            log.warn("请求体过大, 已拒绝: path={}, size={}bytes, max={}bytes",
                    request.getURI().getPath(), contentLength, maxSize);
            return payloadTooLarge(exchange, maxSize);
        }

        return chain.filter(exchange);
    }

    /**
     * 返回 413 Payload Too Large
     */
    private Mono<Void> payloadTooLarge(ServerWebExchange exchange, long maxSize) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        long maxMb = maxSize / (1024 * 1024);
        RestResult<?> result = RestResult.fail(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "请求体过大, 最大允许 " + maxMb + "MB"
        );
        byte[] bytes = JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 过滤器链顺序: TraceLog(-1) → RequestSize(0) → RateLimit(1) → JwtAuth(2) → Canary(3)
        return 0;
    }
}
