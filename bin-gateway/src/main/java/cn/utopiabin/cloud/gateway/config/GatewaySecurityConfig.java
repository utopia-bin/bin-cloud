package cn.utopiabin.cloud.gateway.config;

import cn.utopiabin.cloud.common.context.GatewayContextProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

/** 网关启动安全校验和通用响应安全头配置。 */
@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SmartInitializingSingleton gatewayContextSecretValidator(
            GatewayContextProperties properties) {
        return () -> {
            String secret = properties.getSigningSecret();
            if (secret == null || secret.length() < 32) {
                throw new IllegalStateException(
                        "gateway.context.signing-secret must contain at least 32 characters");
            }
        };
    }

    @Bean
    public WebFilter securityResponseHeadersFilter() {
        return (exchange, chain) -> {
            var headers = exchange.getResponse().getHeaders();
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Referrer-Policy", "no-referrer");
            headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            if (exchange.getRequest().getURI().getScheme() != null
                    && exchange.getRequest().getURI().getScheme().equalsIgnoreCase("https")) {
                headers.set("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains");
            }
            return chain.filter(exchange);
        };
    }
}
