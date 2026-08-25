package cn.utopiabin.cloud.gateway.loadbalancer;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanaryServiceInstanceListSupplierTest {

    @Test
    void readsCanaryHeaderFromRequestDataContext() {
        ServiceInstance stable = instance("stable", false);
        ServiceInstance canary = instance("canary", true);
        ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
        when(delegate.get(any())).thenReturn(Flux.just(List.of(stable, canary)));
        CanaryServiceInstanceListSupplier supplier =
                new CanaryServiceInstanceListSupplier(delegate, false);

        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonConstants.HEADER_CANARY, "true");
        RequestData data = new RequestData(HttpMethod.GET, URI.create("http://service/test"),
                headers, new HttpHeaders(), Map.of());
        var request = new DefaultRequest<>(new RequestDataContext(data));

        StepVerifier.create(supplier.get(request))
                .expectNext(List.of(canary))
                .verifyComplete();
    }

    @Test
    void stableTrafficNeverFallsBackToCanaryOnlyInstances() {
        ServiceInstance canary = instance("canary", true);
        ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
        when(delegate.get(any())).thenReturn(Flux.just(List.of(canary)));
        CanaryServiceInstanceListSupplier supplier =
                new CanaryServiceInstanceListSupplier(delegate, true);
        RequestData data = new RequestData(HttpMethod.GET, URI.create("http://service/test"),
                new HttpHeaders(), new HttpHeaders(), Map.of());

        StepVerifier.create(supplier.get(new DefaultRequest<>(new RequestDataContext(data))))
                .expectNext(List.of())
                .verifyComplete();
    }

    private ServiceInstance instance(String id, boolean canary) {
        DefaultServiceInstance instance = new DefaultServiceInstance(
                id, "service", "localhost", 8080, false,
                Map.of("canary", Boolean.toString(canary)));
        return instance;
    }
}
