package cn.utopiabin.cloud.gateway.handler;

import cn.utopiabin.cloud.common.rest.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 熔断降级处理器
 * <p>
 * 当 Resilience4j CircuitBreaker 触发熔断时, 返回此兜底响应。
 * 匹配 RouteConfig 中配置的 fallbackUri 路径。
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
public class CircuitBreakerFallbackHandler {

    /**
     * 通用降级入口: forward:/fallback/{service}
     */
    @RequestMapping(value = "/fallback/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RestResult<Map<String, String>>>> fallback(@PathVariable String service) {
        log.warn("[CircuitBreaker] 服务熔断降级: service={}", service);
        RestResult<Map<String, String>> body = RestResult.<Map<String, String>>fail(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "服务 [" + service + "] 暂时不可用, 已触发熔断保护")
                .data(Map.of("service", service, "status", "circuit_open"));
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }

    /**
     * 通用降级入口 (无 service 参数)
     */
    @RequestMapping(value = "/fallback", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RestResult<String>>> fallbackDefault() {
        log.warn("[CircuitBreaker] 通用熔断降级触发");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(RestResult.fail(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "服务暂时不可用, 已触发熔断保护")));
    }
}
