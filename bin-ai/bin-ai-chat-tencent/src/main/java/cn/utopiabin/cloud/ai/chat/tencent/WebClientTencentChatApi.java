package cn.utopiabin.cloud.ai.chat.tencent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

final class WebClientTencentChatApi implements TencentChatApi {

    private static final String REQUEST_ID_HEADER = "X-REQUEST-ID";
    private static final String DONE = "[DONE]";
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final TypeReference<Map<String, Object>> DATA_TYPE = new TypeReference<>() {
    };

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String chatPath;

    WebClientTencentChatApi(TencentChatProperties properties, WebClient.Builder webClientBuilder,
                            ObjectMapper objectMapper) {
        Assert.hasText(properties.getBaseUrl(), "Tencent base URL cannot be empty");
        Assert.hasText(properties.getChatPath(), "Tencent chat path cannot be empty");
        this.chatPath = properties.getChatPath();
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.clone()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "bin-cloud-spring-ai")
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(Math.toIntExact(properties.getMaxInMemorySize().toBytes())))
                .build();
    }

    @Override
    public Flux<TencentChatProtocol.ChatResponseChunk> stream(TencentChatProtocol.ChatRequest request) {
        Assert.notNull(request, "request cannot be null");
        Assert.hasText(request.appKey(), "Tencent app key cannot be empty");
        return this.webClient.post()
                .uri(this.chatPath)
                .headers(headers -> {
                    if (request.requestId() != null) {
                        headers.set(REQUEST_ID_HEADER, request.requestId());
                    }
                })
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> TencentChatException.http(response.statusCode(), body)))
                .bodyToFlux(SSE_TYPE)
                .filter(event -> event.data() != null && !DONE.equals(event.data()))
                .map(this::decode)
                .filter(Objects::nonNull);
    }

    private TencentChatProtocol.ChatResponseChunk decode(ServerSentEvent<String> event) {
        try {
            Map<String, Object> data = this.objectMapper.readValue(event.data(), DATA_TYPE);
            String eventType = event.event();
            if (data.get("event") instanceof String envelopedEvent && data.get("data") instanceof Map<?, ?> nestedData) {
                eventType = eventType == null ? envelopedEvent : eventType;
                @SuppressWarnings("unchecked")
                Map<String, Object> typedNestedData = (Map<String, Object>) nestedData;
                data = typedNestedData;
            }
            if (eventType == null) {
                eventType = Objects.toString(data.get("Type"), null);
            }
            return new TencentChatProtocol.ChatResponseChunk(eventType, data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to decode Tencent SSE event", exception);
        }
    }
}
