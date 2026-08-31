package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.context.GatewayContextProperties;
import cn.utopiabin.cloud.common.context.GatewayContextSigner;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

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
    private final GatewayContextProperties gatewayContextProperties;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ApplicationSessionValidator sessionValidator;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 所有入口先移除客户端可伪造的内部身份头，白名单请求也不能保留这些头。
        ServerWebExchange sanitizedExchange = removeUntrustedIdentityHeaders(exchange);
        String path = sanitizedExchange.getRequest().getURI().getPath();
        if (path.startsWith("/platform/internal/") || path.startsWith("/internal/")) return unauthorized(sanitizedExchange,"内部接口不可通过公共网关访问");

        // 1. 白名单路径直接放行
        if (isWhitePath(path)) {
            return chain.filter(sanitizedExchange);
        }

        // 2. 提取 Token
        String token = extractToken(sanitizedExchange.getRequest());
        if (token == null) {
            return unauthorized(sanitizedExchange, "缺少有效的认证 Token");
        }

        // 3. 检查 Token 黑名单 (Redis)
        return checkTokenBlacklist(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return unauthorized(sanitizedExchange, "Token 已被注销, 请重新登录");
                    }

                    // 4. 解析并校验 Token (含租户身份校验) —— 异步执行, 避免签名验签阻塞事件循环
                    return Mono.fromCallable(() -> JwtUtil.parse(token, gatewayConfig.getJwtSecret()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(payload -> {
                                if (payload == null) {
                                    return unauthorized(sanitizedExchange, "Token 无效或已过期");
                                }
                                String audience = expectedAudience(sanitizedExchange, path);
                                if (audience==null || !audience.equals(payload.getAudience()) || StrUtil.isBlank(payload.getSessionId())) {
                                    return unauthorized(sanitizedExchange,"Token不属于目标应用或为旧登录态，请重新登录");
                                }
                                return sessionValidator.valid(token,audience).flatMap(valid -> {
                                if (!valid) return unauthorized(sanitizedExchange,"会话已失效或会话校验服务不可用");

                                // 5. 将用户信息注入请求头传递给下游
                                String roles = payload.getRoles() != null
                                        ? String.join(",", payload.getRoles()) : "";
                                long timestamp = System.currentTimeMillis();
                                String signature = GatewayContextSigner.sign(
                                        gatewayContextProperties.getSigningSecret(), timestamp,
                                        payload.getUserId(), payload.getUsername(), payload.getTenantId(), roles);
                                ServerHttpRequest modifiedRequest = sanitizedExchange.getRequest().mutate()
                                        .header(CommonConstants.HEADER_USER_ID, payload.getUserId())
                                        .header(CommonConstants.HEADER_USER_NAME, payload.getUsername())
                                        .header(CommonConstants.HEADER_TENANT_ID, payload.getTenantId())
                                        .header(CommonConstants.HEADER_USER_ROLES, roles)
                                        .header(CommonConstants.HEADER_TOKEN, token)
                                        .header(CommonConstants.HEADER_GATEWAY_TIMESTAMP, String.valueOf(timestamp))
                                        .header(CommonConstants.HEADER_GATEWAY_SIGNATURE, signature)
                                        .build();

                                log.debug("JWT 鉴权通过: userId={}, tenantId={}, path={}",
                                        payload.getUserId(), payload.getTenantId(), path);
                                return chain.filter(sanitizedExchange.mutate().request(modifiedRequest).build());
                                });
                            })
                            // 捕获 JWT 解析过程中的任何异常, 统一返回 401 而非 500
                            .onErrorResume(e -> {
                                log.warn("JWT 解析异常: {}", e.getMessage());
                                return unauthorized(sanitizedExchange, "Token 无效、已过期或网关上下文签名未配置");
                            });
                });
    }

    private ServerWebExchange removeUntrustedIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(CommonConstants.HEADER_USER_ID);
                    headers.remove(CommonConstants.HEADER_USER_NAME);
                    headers.remove(CommonConstants.HEADER_TENANT_ID);
                    headers.remove(CommonConstants.HEADER_USER_ROLES);
                    headers.remove(CommonConstants.HEADER_TOKEN);
                    headers.remove(CommonConstants.HEADER_GATEWAY_TIMESTAMP);
                    headers.remove(CommonConstants.HEADER_GATEWAY_SIGNATURE);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private String expectedAudience(ServerWebExchange exchange,String path) {
        if(path.startsWith("/admin/") || path.startsWith("/platform/")) return "platform-console";
        if(path.startsWith("/open/")) return "learning-workbench";
        org.springframework.cloud.gateway.route.Route route = exchange.getAttribute(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        Object configured=route==null?null:route.getMetadata().get("applicationAudience");
        return configured instanceof String text && !text.isBlank()?text:null;
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

        String blacklistKey = GatewayConstants.TOKEN_BLACKLIST_PREFIX + tokenDigest(token);

        return reactiveRedisTemplate.hasKey(blacklistKey)
                .defaultIfEmpty(false)
                .doOnNext(isBlacked -> {
                    if (isBlacked) {
                        log.info("Token 命中黑名单: key={}", blacklistKey);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Token 黑名单服务不可用, failClosed={}",
                            gatewayConfig.isTokenBlacklistFailClosed(), error);
                    return Mono.just(gatewayConfig.isTokenBlacklistFailClosed());
                });
    }

    /**
     * 判断是否为白名单路径
     */
    private boolean isWhitePath(String path) {
        List<String> whitePaths = gatewayConfig.getWhitePaths();
        if (whitePaths != null && !whitePaths.isEmpty()) {
            return Stream.concat(GatewayConstants.WHITE_PATHS.stream(), whitePaths.stream())
                    .anyMatch(p -> PATH_MATCHER.match(p, path));
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
        boolean websocketUpgrade = "websocket".equalsIgnoreCase(
                request.getHeaders().getFirst(HttpHeaders.UPGRADE));
        boolean allowedWebsocketPath = gatewayConfig.getWebsocketTokenPaths().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, request.getURI().getPath()));
        return websocketUpgrade && allowedWebsocketPath
                ? request.getQueryParams().getFirst(CommonConstants.TOKEN_PARAM) : null;
    }

    private String tokenDigest(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
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
