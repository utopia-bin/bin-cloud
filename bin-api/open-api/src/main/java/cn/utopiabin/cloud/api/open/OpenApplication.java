package cn.utopiabin.cloud.api.open;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 开放端启动类
 */
@SpringBootApplication
@EnableDubbo(scanBasePackages = "cn.utopiabin.cloud.api.open")
public class OpenApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenApplication.class, args);
    }
}
