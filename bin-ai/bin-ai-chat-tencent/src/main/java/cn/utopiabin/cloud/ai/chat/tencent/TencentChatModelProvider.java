package cn.utopiabin.cloud.ai.chat.tencent;

import cn.utopiabin.cloud.ai.core.chat.ChatModelProvider;
import org.springframework.ai.chat.model.ChatModel;

record TencentChatModelProvider(ChatModel chatModel) implements ChatModelProvider {

    @Override
    public String id() {
        return "tencent";
    }
}
