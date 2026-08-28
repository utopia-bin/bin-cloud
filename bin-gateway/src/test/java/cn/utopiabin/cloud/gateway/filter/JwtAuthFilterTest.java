package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.context.GatewayContextProperties;
import cn.utopiabin.cloud.common.context.GatewayContextSigner;
import cn.utopiabin.cloud.gateway.config.GatewayConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    private static final String JWT_SECRET = "jwt-secret-0123456789abcdef0123456789abcdef";
    private static final String CONTEXT_SECRET = "context-0123456789abcdef0123456789abcdef";

    @Test
    void whitelistRequestCannotCarryForgedIdentityHeaders() {
        var filter = filter();
        var request = MockServerHttpRequest.post("/admin/auth/login")
                .header(CommonConstants.HEADER_USER_ID, "forged")
                .header(CommonConstants.HEADER_TENANT_ID, "999")
                .header(CommonConstants.HEADER_GATEWAY_SIGNATURE, "forged")
                .build();
        var exchange = MockServerWebExchange.from(request);
        var downstreamHeaders = new AtomicReference<HttpHeaders>();
        GatewayFilterChain chain = current -> {
            downstreamHeaders.set(current.getRequest().getHeaders());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(2));

        assertNotNull(downstreamHeaders.get());
        assertNull(downstreamHeaders.get().getFirst(CommonConstants.HEADER_USER_ID));
        assertNull(downstreamHeaders.get().getFirst(CommonConstants.HEADER_TENANT_ID));
        assertNull(downstreamHeaders.get().getFirst(CommonConstants.HEADER_GATEWAY_SIGNATURE));
    }

    @Test
    void prefixedOpenApiDocumentRequestDoesNotRequireToken() {
        var filter = filter();
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin/v3/api-docs").build());
        var invoked = new AtomicReference<Boolean>(false);
        GatewayFilterChain chain = current -> {
            invoked.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(2));

        assertEquals(Boolean.TRUE, invoked.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void authenticatedRequestCarriesVerifiableGatewayContext() {
        var filter = filter();
        String token = token();
        var request = MockServerHttpRequest.get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(CommonConstants.HEADER_USER_ID, "forged")
                .build();
        var exchange = MockServerWebExchange.from(request);
        var downstreamHeaders = new AtomicReference<HttpHeaders>();
        GatewayFilterChain chain = current -> {
            downstreamHeaders.set(current.getRequest().getHeaders());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(2));

        HttpHeaders headers = downstreamHeaders.get();
        assertNotNull(headers);
        assertEquals("10", headers.getFirst(CommonConstants.HEADER_USER_ID));
        assertEquals("7", headers.getFirst(CommonConstants.HEADER_TENANT_ID));
        assertTrue(GatewayContextSigner.verify(
                CONTEXT_SECRET,
                headers.getFirst(CommonConstants.HEADER_GATEWAY_TIMESTAMP),
                headers.getFirst(CommonConstants.HEADER_GATEWAY_SIGNATURE),
                "10", "alice", "7", "tenant-admin",
                Duration.ofSeconds(30)));
    }

    @SuppressWarnings("unchecked")
    private JwtAuthFilter filter() {
        var gatewayConfig = new GatewayConfig();
        gatewayConfig.setJwtSecret(JWT_SECRET);
        gatewayConfig.setTokenBlacklistEnabled(false);
        var contextProperties = new GatewayContextProperties();
        contextProperties.setSigningSecret(CONTEXT_SECRET);
        return new JwtAuthFilter(gatewayConfig, contextProperties, mock(ReactiveRedisTemplate.class));
    }

    private String token() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("userId", "10")
                .claim("username", "alice")
                .claim("tenantId", "7")
                .claim("roles", List.of("tenant-admin"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
