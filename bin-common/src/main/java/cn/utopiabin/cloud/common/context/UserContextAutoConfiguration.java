package cn.utopiabin.cloud.common.context;

import jakarta.servlet.http.HttpServlet;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

/**
 * 用户上下文自动装配
 * <p>
 * 仅在 Servlet 容器 (spring-boot-starter-web) 环境下激活:
 * <ol>
 *   <li>注册 {@link UserContextFilter} —— 从请求头自动提取用户信息到 ThreadLocal</li>
 * </ol>
 * <p>
 * Dubbo 透传过滤器 {@link UserContextDubboFilter} 通过 Dubbo SPI 自动注册,
 * 无需在此手动声明 Bean。
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(HttpServlet.class)
public class UserContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }
}
