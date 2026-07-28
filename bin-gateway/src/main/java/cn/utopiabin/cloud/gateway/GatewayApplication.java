package cn.utopiabin.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类
 * <p>
 * 基于 Spring Cloud Gateway + Nacos 服务发现,
 * 提供统一路由、JWT 鉴权、限流、熔断、灰度等网关能力。
 *
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan("cn.utopiabin.cloud.gateway.config")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
