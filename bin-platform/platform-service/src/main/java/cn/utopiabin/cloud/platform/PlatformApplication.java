package cn.utopiabin.cloud.platform;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 平台基座服务启动类
 */
@SpringBootApplication
@EnableDubbo(scanBasePackages = "cn.utopiabin.cloud.platform.api.impl")
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
