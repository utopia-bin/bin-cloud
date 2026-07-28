package cn.utopiabin.cloud.gateway.config;

import cn.utopiabin.cloud.gateway.loadbalancer.CanaryServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 负载均衡配置
 * <p>
 * 注册灰度感知的服务实例列表供应器, 使 {@code CanaryFilter} 设置的
 * {@code X-Canary} 请求头能实际作用于 LoadBalancer 的实例选择逻辑。
 * <p>
 * 链路: CanaryFilter 设置 X-Canary 头 → LoadBalancer 调用 get(Request)
 * → CanaryServiceInstanceListSupplier 过滤出 canary 实例
 *
 * @since 1.0.0
 */
@Configuration
public class LoadBalancerConfig {

    /**
     * 灰度感知实例列表供应器 (装饰默认的 Nacos 服务发现供应器)
     */
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return new CanaryServiceInstanceListSupplier(
                ServiceInstanceListSupplier.builder()
                        .withDiscoveryClient()
                        .withCaching()
                        .build(context));
    }
}
