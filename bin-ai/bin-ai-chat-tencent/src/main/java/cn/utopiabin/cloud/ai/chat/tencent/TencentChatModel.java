package cn.utopiabin.cloud.ai.chat.tencent;

import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems")
public final class TencentChatModel implements ChatModel {

    private final TencentChatApi chatApi;
    private final TencentChatOptions defaultOptions;

    TencentChatModel(TencentChatApi chatApi, TencentChatOptions defaultOptions) {
        Assert.notNull(chatApi, "Tencent chat API cannot be null");
        Assert.notNull(defaultOptions, "Tencent default options cannot be null");
        this.chatApi = chatApi;
        this.defaultOptions = defaultOptions;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        TencentChatProtocol.ChatRequest request = this.createRequest(prompt, this.mergeOptions(prompt.getOptions()));
        List<TencentChatProtocol.ChatResponseChunk> chunks = this.chatApi.stream(request)
                .map(this::throwIfError)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);
        return this.aggregate(chunks);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        TencentChatProtocol.ChatRequest request = this.createRequest(prompt, this.mergeOptions(prompt.getOptions()));
        return this.chatApi.stream(request)
                .map(this::throwIfError)
                .map(this::toChatResponse);
    }

    TencentChatProtocol.ChatRequest createRequest(Prompt prompt, TencentChatOptions options) {
        Assert.notNull(prompt, "prompt cannot be null");
        String systemRole = prompt.getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));

        List<TencentChatProtocol.Content> contents = new ArrayList<>();
        for (Message message : prompt.getInstructions()) {
            if (message instanceof UserMessage userMessage) {
                contents.addAll(this.convertUserMessage(userMessage));
            } else if (message.getMessageType() != MessageType.SYSTEM) {
                throw new IllegalArgumentException("Tencent ADP V2 does not accept prompt message type: "
                        + message.getMessageType());
            }
        }
        Assert.notEmpty(contents, "Tencent prompt must contain user content");

        return new TencentChatProtocol.ChatRequest(
                StringUtils.hasText(options.getRequestId()) ? options.getRequestId() : UUID.randomUUID().toString(),
                options.getConversationId(),
                options.getAppKey(),
                options.getVisitorId(),
                contents,
                options.getStreamingThrottle(),
                StringUtils.hasText(systemRole) ? systemRole : null,
                options.getIncremental(),
                options.getSearchNetwork(),
                options.getModel(),
                options.getStream(),
                options.getWorkflowStatus(),
                options.getEnableMultiIntent(),
                options.getGenerateAgain());
    }

    private TencentChatOptions mergeOptions(ChatOptions runtimeOptions) {
        if (runtimeOptions == null) {
            return this.defaultOptions.copy();
        }
        if (runtimeOptions instanceof TencentChatOptions tencentOptions) {
            return ModelOptionsUtils.merge(tencentOptions, this.defaultOptions, TencentChatOptions.class);
        }
        TencentChatOptions options = this.defaultOptions.copy();
        if (runtimeOptions.getModel() != null) {
            options.setModel(runtimeOptions.getModel());
        }
        return options;
    }

    private List<TencentChatProtocol.Content> convertUserMessage(UserMessage message) {
        List<TencentChatProtocol.Content> contents = new ArrayList<>();
        if (StringUtils.hasText(message.getText())) {
            contents.add(TencentChatProtocol.Content.text(message.getText()));
        }
        if (!CollectionUtils.isEmpty(message.getMedia())) {
            message.getMedia().forEach(media -> {
                if ("image".equalsIgnoreCase(media.getMimeType().getType())) {
                    contents.add(TencentChatProtocol.Content.image(this.toPublicUrl(media.getData())));
                }
            });
        }
        Map<String, Object> metadata = message.getMetadata();
        if (!CollectionUtils.isEmpty(metadata)
                && metadata.get("CustomVariables") instanceof Map<?, ?> customVariables) {
            Map<String, String> variables = new LinkedHashMap<>();
            customVariables.forEach((key, value) -> variables.put(String.valueOf(key), String.valueOf(value)));
            contents.add(TencentChatProtocol.Content.customVariables(variables));
        }
        return contents;
    }

    private String toPublicUrl(Object data) {
        if (data instanceof URI || data instanceof URL || data instanceof String) {
            return data.toString();
        }
        if (data instanceof Resource resource) {
            try {
                return resource.getURL().toString();
            } catch (IOException exception) {
                throw new IllegalArgumentException("Tencent image media must have a public URL", exception);
            }
        }
        throw new IllegalArgumentException("Tencent image media must be a URL, URI, String, or URL-backed Resource");
    }

    private TencentChatProtocol.ChatResponseChunk throwIfError(TencentChatProtocol.ChatResponseChunk chunk) {
        if (!TencentChatProtocol.EVENT_ERROR.equals(chunk.event())) {
            return chunk;
        }
        Map<String, Object> error = asMap(chunk.data().get("Error"));
        throw TencentChatException.event(
                Objects.toString(error.get("Code"), null),
                Objects.toString(error.get("Message"), null),
                Objects.toString(error.get("RequestId"), null));
    }

    private ChatResponse aggregate(List<TencentChatProtocol.ChatResponseChunk> chunks) {
        StringBuilder answer = new StringBuilder();
        TencentChatProtocol.ChatResponseChunk terminal = null;
        for (TencentChatProtocol.ChatResponseChunk chunk : chunks) {
            terminal = chunk;
            String text = this.extractText(chunk);
            if (!StringUtils.hasLength(text)) {
                continue;
            }
            if (TencentChatProtocol.EVENT_TEXT_REPLACE.equals(chunk.event())
                    || TencentChatProtocol.EVENT_RESPONSE_COMPLETED.equals(chunk.event())) {
                answer.setLength(0);
            }
            answer.append(text);
        }
        Map<String, Object> metadata = terminal == null
                ? Map.of("provider", "tencent", "event", "empty")
                : this.metadata(terminal);
        return createResponse(answer.toString(), metadata);
    }

    private ChatResponse toChatResponse(TencentChatProtocol.ChatResponseChunk chunk) {
        return createResponse(this.extractText(chunk), this.metadata(chunk));
    }

    private String extractText(TencentChatProtocol.ChatResponseChunk chunk) {
        if (TencentChatProtocol.EVENT_TEXT_DELTA.equals(chunk.event())
                || TencentChatProtocol.EVENT_TEXT_REPLACE.equals(chunk.event())) {
            return Objects.toString(chunk.data().get("Text"), "");
        }
        if (!TencentChatProtocol.EVENT_RESPONSE_COMPLETED.equals(chunk.event())) {
            return "";
        }
        Map<String, Object> response = asMap(chunk.data().get("Response"));
        for (Object messageValue : asList(response.get("Messages"))) {
            Map<String, Object> message = asMap(messageValue);
            if (!"reply".equals(message.get("Type"))) {
                continue;
            }
            for (Object contentValue : asList(message.get("Contents"))) {
                Map<String, Object> content = asMap(contentValue);
                if ("text".equals(content.get("Type"))) {
                    return Objects.toString(content.get("Text"), "");
                }
            }
        }
        return "";
    }

    private Map<String, Object> metadata(TencentChatProtocol.ChatResponseChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "tencent");
        metadata.put("event", Objects.requireNonNullElse(chunk.event(), "unknown"));
        metadata.put("data", chunk.data());
        return metadata;
    }

    private static ChatResponse createResponse(String text, Map<String, Object> metadata) {
        AssistantMessage message = AssistantMessage.builder()
                .content(Objects.requireNonNullElse(text, ""))
                .properties(metadata)
                .build();
        return new ChatResponse(List.of(new Generation(message, ChatGenerationMetadata.NULL)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }
}
