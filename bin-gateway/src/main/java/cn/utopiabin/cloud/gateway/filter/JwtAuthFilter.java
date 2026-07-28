package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.gateway.config.GatewayConfig;
import cn.utopiabin.cloud.gateway.model.GatewayConstants;
import cn.utopiabin.cloud.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权全局过滤器 (租户感知)
 * <p>
 * 1. 白名单路径直接放行<br>
 * 2. 提取 Token 并检查 Redis 黑名单 (支持主动注销/踢人)<br>
 * 3. 解析 JWT, 校验签名、过期时间、租户身份<br>
 * 4. 将用户信息 (userId, username, tenantId, roles) 注入请求头传递给下游服务
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final GatewayConfig gatewayConfig;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单路径直接放行
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Token
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange, "缺少有效的认证 Token");
        }

        // 3. 检查 Token 黑名单 (Redis)
        return checkTokenBlacklist(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return unauthorized(exchange, "Token 已被注销, 请重新登录");
                    }

                    // 4. 解析并校验 Token (含租户身份校验) —— 异步执行, 避免签名验签阻塞事件循环
                    return Mono.fromCallable(() -> JwtUtil.parse(token, gatewayConfig.getJwtSecret()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(payload -> {
                                if (payload == null) {
                                    return unauthorized(exchange, "Token 无效或已过期");
                                }

                                // 5. 将用户信息注入请求头传递给下游
                                //    安全: 先移除客户端可能伪造的身份头, 再注入网关验签后的真实值,
                                //    避免 mutate().header() 追加导致伪造头与真实头并存
                                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                        .headers(h -> {
                                            h.remove(CommonConstants.HEADER_USER_ID);
                                            h.remove(CommonConstants.HEADER_USER_NAME);
                                            h.remove(CommonConstants.HEADER_TENANT_ID);
                                            h.remove(CommonConstants.HEADER_USER_ROLES);
                                            h.remove(CommonConstants.HEADER_TOKEN);
                                        })
                                        .header(CommonConstants.HEADER_USER_ID, payload.getUserId())
                                        .header(CommonConstants.HEADER_USER_NAME, payload.getUsername())
                                        .header(CommonConstants.HEADER_TENANT_ID, payload.getTenantId())
                                        .header(CommonConstants.HEADER_USER_ROLES,
                                                payload.getRoles() != null ? String.join(",", payload.getRoles()) : "")
                                        .header(CommonConstants.HEADER_TOKEN, token)
                                        .build();

                                log.debug("JWT 鉴权通过: userId={}, tenantId={}, path={}",
                                        payload.getUserId(), payload.getTenantId(), path);
                                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                            })
                            // 捕获 JWT 解析过程中的任何异常, 统一返回 401 而非 500
                            .onErrorResume(e -> {
                                log.warn("JWT 解析异常: {}", e.getMessage());
                                return unauthorized(exchange, "Token 无效或已过期");
                            });
                });
    }

    /**
     * 检查 Token 是否在 Redis 黑名单中
     *
     * @param token JWT Token
     * @return true=已拉黑, false=正常
     */
    private Mono<Boolean> checkTokenBlacklist(String token) {
        if (!gatewayConfig.isTokenBlacklistEnabled()) {
            return Mono.just(false);
        }

        // 取 Token 的后 16 位作为黑名单 Key 的快速索引
        String suffix = token.length() > 16 ? token.substring(token.length() - 16) : token;
        String blacklistKey = GatewayConstants.TOKEN_BLACKLIST_PREFIX + suffix;

        return reactiveRedisTemplate.hasKey(blacklistKey)
                .defaultIfEmpty(false)
                .doOnNext(isBlacked -> {
                    if (isBlacked) {
                        log.info("Token 命中黑名单: key={}", blacklistKey);
                    }
                });
    }

    /**
     * 判断是否为白名单路径
     */
    private boolean isWhitePath(String path) {
        List<String> whitePaths = gatewayConfig.getWhitePaths();
        if (whitePaths != null && !whitePaths.isEmpty()) {
            return whitePaths.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
        }
        return GatewayConstants.WHITE_PATHS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    /**
     * 从请求中提取 JWT Token
     */
    private String extractToken(ServerHttpRequest request) {
        // 优先从 Authorization 头提取
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = JwtUtil.extractToken(authHeader);
        if (!StrUtil.isBlank(token)) {
            return token;
        }
        // 备选: 从查询参数 token 提取
        return request.getQueryParams().getFirst(CommonConstants.TOKEN_PARAM);
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        RestResult<?> result = RestResult.fail(HttpStatus.UNAUTHORIZED.value(), message);
        byte[] bytes = JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 过滤器链顺序: TraceLog(-1) → RequestSize(0) → RateLimit(1) → JwtAuth(2) → Canary(3)
        return 2;
    }
}
