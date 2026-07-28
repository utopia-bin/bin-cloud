package cn.utopiabin.cloud.gateway.loadbalancer;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 灰度感知的服务实例列表供应器
 * <p>
 * 包装默认的 {@link ServiceInstanceListSupplier}, 根据请求头 {@code X-Canary}
 * 过滤出带有 {@code canary=true} Nacos 元数据的实例:
 * <ul>
 *   <li>请求头 X-Canary=true → 仅选择 canary 实例</li>
 *   <li>无 X-Canary 头 → 仅选择非 canary 实例 (稳定版本)</li>
 *   <li>canary 实例不存在时 → 降级到全部实例 (保证可用性)</li>
 * </ul>
 * <p>
 * 下游服务需在 Nacos 注册时设置元数据 {@code canary=true} 才能被灰度路由选中。
 *
 * @since 1.0.0
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class CanaryServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

    /** Nacos 实例元数据中的灰度标记 Key */
    private static final String META_CANARY = "canary";

    public CanaryServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
        super(delegate);
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return getDelegate().get();
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return getDelegate().get(request)
                .map(instances -> filterByCanary(instances, request));
    }

    /**
     * 根据 X-Canary 请求头过滤实例
     */
    private List<ServiceInstance> filterByCanary(List<ServiceInstance> instances, Request request) {
        if (instances == null || instances.isEmpty()) {
            return instances;
        }

        boolean isCanary = isCanaryRequest(request);

        List<ServiceInstance> filtered = instances.stream()
                .filter(instance -> {
                    boolean isInstanceCanary = "true".equalsIgnoreCase(
                            instance.getMetadata().get(META_CANARY));
                    return isInstanceCanary == isCanary;
                })
                .toList();

        // 降级: 无匹配实例时返回全部, 保证请求不因灰度配置缺失而失败
        if (filtered.isEmpty()) {
            log.debug("灰度实例未匹配, 降级到全部实例: serviceId={}, isCanaryRequest={}",
                    getServiceId(), isCanary);
            return instances;
        }

        log.debug("灰度路由选择: serviceId={}, isCanary={}, total={}, selected={}",
                getServiceId(), isCanary, instances.size(), filtered.size());
        return filtered;
    }

    /**
     * 从 LoadBalancer 请求上下文中提取 X-Canary 头
     */
    private boolean isCanaryRequest(Request request) {
        if (request == null) {
            return false;
        }
        Object context = request.getContext();
        if (context instanceof HttpRequest httpRequest) {
            return "true".equalsIgnoreCase(
                    httpRequest.getHeaders().getFirst(CommonConstants.HEADER_CANARY));
        }
        // 兼容: 部分版本 context 为 HttpHeaders
        if (context instanceof org.springframework.http.HttpHeaders headers) {
            return "true".equalsIgnoreCase(
                    headers.getFirst(CommonConstants.HEADER_CANARY));
        }
        return false;
    }
}
