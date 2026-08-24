package cn.utopiabin.cloud.ai.core.chat;

import cn.utopiabin.cloud.ai.core.AiProvider;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Chat capability supplied by an AI provider.
 */
public interface ChatModelProvider extends AiProvider {

    ChatModel chatModel();
}
