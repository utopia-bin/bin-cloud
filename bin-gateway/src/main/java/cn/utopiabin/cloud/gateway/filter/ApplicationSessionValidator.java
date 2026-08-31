package cn.utopiabin.cloud.gateway.filter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Component
public class ApplicationSessionValidator {
    private final WebClient client;
    public ApplicationSessionValidator(@Qualifier("sessionWebClient") WebClient.Builder builder) { client=builder.build(); }
    public Mono<Boolean> valid(String token,String audience) {
        return client.get().uri("http://platform-service/internal/sso/validate")
                .header("Authorization","Bearer "+token).header("X-Expected-Audience",audience)
                .exchangeToMono(response->response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful()))
                .timeout(Duration.ofSeconds(3)).onErrorReturn(false);
    }
}
