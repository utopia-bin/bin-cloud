package cn.utopiabin.cloud.gateway.handler;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.gateway.filter.RequestSizeFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局异常处理器 (WebFlux 反应式)
 * <p>
 * 统一捕获网关层所有未处理异常, 返回标准 RestResult JSON 格式。
 * 涵盖: 鉴权失败、限流、熔断、路由错误、服务不可达等场景。
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@SuppressWarnings("NullableProblems")
public class GlobalExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var response = exchange.getResponse();

        // 如果响应已提交, 无法再修改
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String message;

        // 根据异常类型确定状态码和消息
        NotFoundException notFound = findCause(ex, NotFoundException.class);
        ResponseStatusException responseStatus = findCause(ex, ResponseStatusException.class);
        if (notFound != null) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "目标服务暂时不可用";
        } else if (responseStatus != null) {
            status = HttpStatus.valueOf(responseStatus.getStatusCode().value());
            message = responseStatus.getReason() != null
                    ? responseStatus.getReason() : status.getReasonPhrase();
        } else if (findCause(ex, java.util.concurrent.TimeoutException.class) != null) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "请求超时, 请稍后再试";
        } else if (findCause(ex, java.net.ConnectException.class) != null) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "服务暂时不可用";
        } else if (findCause(ex, RequestSizeFilter.RequestPayloadTooLargeException.class) != null
                || findCause(ex, DataBufferLimitException.class) != null) {
            status = HttpStatus.PAYLOAD_TOO_LARGE;
            message = "请求体超过允许的最大限制";
        } else if (findCause(ex, DecodingException.class) != null) {
            status = HttpStatus.BAD_REQUEST;
            message = "请求数据格式错误";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "网关内部错误";
            log.error("[Gateway Error] path={}, traceId={}",
                    exchange.getRequest().getURI().getPath(),
                    exchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_TRACE_ID),
                    ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = exchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_TRACE_ID);
        if (traceId != null) {
            response.getHeaders().set(CommonConstants.HEADER_TRACE_ID, traceId);
        }

        RestResult<?> result = RestResult.fail(status.value(), message);
        byte[] bytes = JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        log.warn("[Gateway Exception] path={}, status={}, message={}",
                exchange.getRequest().getURI().getPath(), status.value(), message);

        return response.writeWith(Mono.just(buffer));
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable cause = throwable;
        while (cause != null) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
            if (cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
        }
        return null;
    }
}
