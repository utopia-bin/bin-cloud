package cn.utopiabin.cloud.gateway.config;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 跨域配置
 *
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的跨域来源 (生产环境应限制具体域名)
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许的 HTTP 方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));
        // 允许携带凭证
        config.setAllowCredentials(true);
        // 预检请求缓存时间 (秒)
        config.setMaxAge(3600L);
        // 暴露的响应头
        config.setExposedHeaders(List.of(
                CommonConstants.HEADER_TRACE_ID,
                CommonConstants.HEADER_USER_ID,
                CommonConstants.HEADER_USER_NAME,
                CommonConstants.HEADER_TENANT_ID
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
