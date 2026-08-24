package cn.utopiabin.cloud.ai.core.chat;

import org.springframework.util.Assert;

import java.util.*;

/**
 * Resolves chat models without coupling callers to vendor-specific implementations.
 */
public final class ChatModelRegistry {

    private final Map<String, ChatModelProvider> providers;

    public ChatModelRegistry(Collection<ChatModelProvider> providers) {
        Assert.notNull(providers, "providers cannot be null");
        Map<String, ChatModelProvider> indexedProviders = new LinkedHashMap<>();
        for (ChatModelProvider provider : providers) {
            Assert.notNull(provider, "provider cannot be null");
            String providerId = normalize(provider.id());
            ChatModelProvider duplicate = indexedProviders.putIfAbsent(providerId, provider);
            if (duplicate != null) {
                throw new IllegalArgumentException("Duplicate chat model provider id: " + providerId);
            }
        }
        this.providers = Collections.unmodifiableMap(indexedProviders);
    }

    public ChatModelProvider get(String providerId) {
        String normalizedId = normalize(providerId);
        ChatModelProvider provider = this.providers.get(normalizedId);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown chat model provider '%s'; available providers: %s"
                    .formatted(normalizedId, this.providers.keySet()));
        }
        return provider;
    }

    public Set<String> providerIds() {
        return this.providers.keySet();
    }

    private static String normalize(String providerId) {
        Assert.hasText(providerId, "provider id cannot be empty");
        return providerId.trim().toLowerCase(Locale.ROOT);
    }
}
