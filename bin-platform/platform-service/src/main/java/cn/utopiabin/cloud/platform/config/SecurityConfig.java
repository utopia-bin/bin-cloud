package cn.utopiabin.cloud.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关配置 (仅 BCrypt 密码编码器, 不引入 Spring Security 全套框架)
 *
 * @since 1.0
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码编码器 (strength=10, log rounds=2^10=1024)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
