package cn.utopiabin.cloud.api.admin;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 管理端启动类
 */
@SpringBootApplication
@EnableDubbo(scanBasePackages = "cn.utopiabin.cloud.api.admin")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
