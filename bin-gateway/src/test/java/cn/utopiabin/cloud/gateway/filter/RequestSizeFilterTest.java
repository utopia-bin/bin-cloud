package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.gateway.config.GatewayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class RequestSizeFilterTest {

    @Test
    void rejectsKnownOversizedPayload() {
        GatewayConfig config = new GatewayConfig();
        config.setMaxRequestSize(3);
        RequestSizeFilter filter = new RequestSizeFilter(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/upload").header("Content-Length", "4").body("test"));

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(413);
    }

    @Test
    void rejectsChunkedOversizedPayloadWhileReadingBody() {
        GatewayConfig config = new GatewayConfig();
        config.setMaxRequestSize(3);
        RequestSizeFilter filter = new RequestSizeFilter(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/upload").body("test"));

        StepVerifier.create(filter.filter(exchange,
                        current -> DataBufferUtils.join(current.getRequest().getBody()).then()))
                .expectError(RequestSizeFilter.RequestPayloadTooLargeException.class)
                .verify();
    }
}
