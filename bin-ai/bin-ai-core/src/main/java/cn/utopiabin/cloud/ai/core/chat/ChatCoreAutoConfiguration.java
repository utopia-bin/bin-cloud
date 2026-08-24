package cn.utopiabin.cloud.ai.core.chat;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class ChatCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ChatModelRegistry chatModelRegistry(List<ChatModelProvider> providers) {
        return new ChatModelRegistry(providers);
    }
}
