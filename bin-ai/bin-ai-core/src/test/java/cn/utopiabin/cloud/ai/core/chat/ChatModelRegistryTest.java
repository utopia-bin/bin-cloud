package cn.utopiabin.cloud.ai.core.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChatModelRegistryTest {

    @Test
    void resolvesProviderWithNormalizedId() {
        ChatModelProvider provider = provider("tencent");
        ChatModelRegistry registry = new ChatModelRegistry(List.of(provider));

        assertThat(registry.get(" TENCENT ")).isSameAs(provider);
        assertThat(registry.providerIds()).containsExactly("tencent");
    }

    @Test
    void rejectsDuplicateProviderIds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChatModelRegistry(List.of(provider("tencent"), provider("TENCENT"))))
                .withMessageContaining("Duplicate chat model provider id");
    }

    @Test
    void reportsAvailableProvidersForUnknownId() {
        ChatModelRegistry registry = new ChatModelRegistry(List.of(provider("tencent")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.get("unknown"))
                .withMessageContaining("available providers: [tencent]");
    }

    private static ChatModelProvider provider(String id) {
        return new ChatModelProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public ChatModel chatModel() {
                return null;
            }
        };
    }
}
