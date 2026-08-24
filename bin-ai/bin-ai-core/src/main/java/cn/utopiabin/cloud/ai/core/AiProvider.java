package cn.utopiabin.cloud.ai.core;

/**
 * Base contract for an AI vendor or infrastructure provider.
 *
 * <p>Capability-specific contracts such as chat, embedding and retrieval extend
 * this interface instead of forcing every provider into a chat-only abstraction.</p>
 */
public interface AiProvider {

    /**
     * Stable, configuration-facing provider identifier.
     */
    String id();
}
