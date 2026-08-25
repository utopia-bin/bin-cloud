package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.gateway.config.GatewayConfig;
import cn.utopiabin.cloud.gateway.model.GatewayConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 基于 Redis 的限流全局过滤器 (滑动窗口 + 令牌桶)
 * <p>
 * 双重限流策略:
 * <ol>
     *   <li>令牌桶限流 (replenish rate + burst capacity): 防止瞬时流量洪峰</li>
 *   <li>分钟限流 (每分钟 per-minute): 防止持续高频请求</li>
 * </ol>
 * <p>
 * 通过 Redis Lua 脚本保证 INCR + EXPIRE 原子性。
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final GatewayConfig gatewayConfig;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    /**
     * 限流 Lua 脚本:
     * <pre>
     * KEYS[1] = 令牌桶状态 Key
     * KEYS[2] = 分钟限流 Key (每分钟窗口)
     * ARGV[1] = 每秒补充令牌数 (replenish rate)
     * ARGV[2] = 令牌桶容量 (burst capacity)
     * ARGV[3] = 分钟上限 (per-minute)
     * 返回 1 = 放行, 0 = 拒绝
     * </pre>
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of(
            """
            local now = redis.call('TIME')
            local nowMs = now[1] * 1000 + math.floor(now[2] / 1000)
            local rate = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'timestamp')
            local tokens = tonumber(bucket[1]) or capacity
            local timestamp = tonumber(bucket[2]) or nowMs
            tokens = math.min(capacity, tokens + ((nowMs - timestamp) / 1000.0 * rate))
            local burstAllowed = tokens >= 1
            if burstAllowed then tokens = tokens - 1 end
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'timestamp', nowMs)
            redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate * 2000))
            local minuteCount = redis.call('INCR', KEYS[2])
            if minuteCount == 1 then
                redis.call('EXPIRE', KEYS[2], 60)
            end
            if not burstAllowed or minuteCount > tonumber(ARGV[3]) then
                return 0
            end
            return 1
            """,
            Long.class
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayConfig.isRateLimitEnabled()) {
            return chain.filter(exchange);
        }

        String clientIp = extractClientIp(exchange.getRequest());
        String burstKey = GatewayConstants.RATE_LIMIT_PREFIX + "burst:" + clientIp;
        String minuteKey = GatewayConstants.RATE_LIMIT_PREFIX + "minute:" + clientIp;

        List<String> keys = List.of(burstKey, minuteKey);
        List<Object> args = List.of(
                gatewayConfig.getRateLimitReplenishRate(),
                gatewayConfig.getRateLimitBurstCapacity(),
                gatewayConfig.getRateLimitPerMinute()
        );

        return reactiveRedisTemplate.execute(RATE_LIMIT_SCRIPT, keys, args)
                .next()
                .flatMap(allowed -> {
                    if (allowed != null && allowed == 1L) {
                        return chain.filter(exchange);
                    }
                    log.warn("限流拦截: ip={}, path={}", clientIp, exchange.getRequest().getURI().getPath());
                    return tooManyRequests(exchange);
                })
                .onErrorResume(ex -> {
                    // Redis 异常时降级放行, 避免限流故障导致服务不可用
                    log.warn("限流检查异常, 降级放行: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    /**
     * 提取客户端真实 IP (支持反向代理场景)
     */
    private String extractClientIp(ServerHttpRequest request) {
        String remoteIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        if (gatewayConfig.getTrustedProxyAddresses().contains(remoteIp)) {
            String ip = request.getHeaders().getFirst("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                return ip.split(",")[0].trim();
            }
            ip = request.getHeaders().getFirst("X-Real-IP");
            if (ip != null && !ip.isBlank()) {
                return ip.trim();
            }
        }
        return remoteIp;
    }

    /**
     * 返回 429 Too Many Requests
     */
    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", "1");

        RestResult<?> result = RestResult.fail(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "请求过于频繁, 请稍后再试"
        );
        byte[] bytes = JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
