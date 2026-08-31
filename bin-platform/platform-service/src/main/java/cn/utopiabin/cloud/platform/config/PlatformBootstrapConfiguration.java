package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.platform.service.PlatformBootstrapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PlatformBootstrapProperties.class)
public class PlatformBootstrapConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "platform.bootstrap", name = "enabled", havingValue = "true")
    ApplicationRunner platformBootstrapRunner(PlatformBootstrapService bootstrapService) {
        return args -> {
            if (bootstrapService.initialize()) {
                log.info("平台管理员初始化成功，事务已提交；请关闭 PLATFORM_BOOTSTRAP_ENABLED 并移除初始密码");
            }
        };
    }
}
