package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.gateway.model.GatewayConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 全链路日志全局过滤器
 * <p>
 * 生成 traceId 注入请求头和 MDC, 记录每个请求的方法、路径、响应状态和耗时。
 * <p>
 * 注意: 响应式上下文中 MDC 基于 ThreadLocal, 跨线程不可见。
 * 本过滤器在每次日志输出前后显式 set/remove MDC, 确保当前线程可见。
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class TraceLogFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String MDC_TRACE_ID = "traceId";
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final long startTime = System.currentTimeMillis();

        // 生成或复用 traceId
        String traceId = exchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_TRACE_ID);
        if (StrUtil.isBlank(traceId) || !VALID_TRACE_ID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String finalTraceId = traceId;

        // 注入 traceId 到请求头, 供下游服务透传
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CommonConstants.HEADER_TRACE_ID, finalTraceId)
                .build();
        final ServerWebExchange finalExchange = exchange.mutate().request(request).build();
        finalExchange.getResponse().getHeaders().set(CommonConstants.HEADER_TRACE_ID, finalTraceId);

        final String path = request.getURI().getPath();
        final String method = request.getMethod().name();

        // 入口日志
        if (shouldNotSkipLog(path)) {
            MDC.put(MDC_TRACE_ID, finalTraceId);
            try {
                log.info("[IN] {} {} | traceId={}", method, path, finalTraceId);
            } finally {
                MDC.remove(MDC_TRACE_ID);
            }
        }

        return chain.filter(finalExchange)
                .doFinally(signalType -> {
                    long cost = System.currentTimeMillis() - startTime;
                    var response = finalExchange.getResponse();
                    int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

                    if (shouldNotSkipLog(path)) {
                        MDC.put(MDC_TRACE_ID, finalTraceId);
                        try {
                            log.info("[OUT] {} {} | status={} | cost={}ms | traceId={}",
                                    method, path, status, cost, finalTraceId);
                        } finally {
                            MDC.remove(MDC_TRACE_ID);
                        }
                    }
                })
                .doOnError(ex -> {
                    if (shouldNotSkipLog(path)) {
                        MDC.put(MDC_TRACE_ID, finalTraceId);
                        try {
                            log.error("[ERR] {} {} | traceId={}", method, path, finalTraceId, ex);
                        } finally {
                            MDC.remove(MDC_TRACE_ID);
                        }
                    }
                });
    }

    /**
     * 判断路径是否需要跳过详细日志
     */
    private boolean shouldNotSkipLog(String path) {
        return GatewayConstants.SKIP_LOG_PATHS.stream()
                .noneMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
