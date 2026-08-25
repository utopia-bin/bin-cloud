package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class TraceLogFilterTest {

    @Test
    void replacesInvalidTraceIdAndReturnsItToClient() {
        TraceLogFilter filter = new TraceLogFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/public/ping")
                        .header(CommonConstants.HEADER_TRACE_ID, "invalid trace value"));

        StepVerifier.create(filter.filter(exchange, current -> Mono.empty())).verifyComplete();

        String traceId = exchange.getResponse().getHeaders().getFirst(CommonConstants.HEADER_TRACE_ID);
        assertThat(traceId).matches("[A-Za-z0-9_-]{8,64}");
        assertThat(traceId).doesNotContain("invalid");
    }
}
