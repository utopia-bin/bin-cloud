package cn.utopiabin.cloud.gateway;

import cn.utopiabin.cloud.common.context.GatewayContextProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import cn.utopiabin.cloud.gateway.config.LoadBalancerConfig;

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
@EnableConfigurationProperties(GatewayContextProperties.class)
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfig.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
