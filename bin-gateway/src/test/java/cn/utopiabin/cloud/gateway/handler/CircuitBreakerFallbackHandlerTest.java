package cn.utopiabin.cloud.gateway.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerFallbackHandlerTest {

    @Test
    void fallbackReturnsServiceUnavailable() {
        var response = new CircuitBreakerFallbackHandler().fallback("admin").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(503);
    }
}
