package cn.utopiabin.cloud.ai.chat.tencent;

import cn.utopiabin.cloud.ai.core.chat.ChatCoreAutoConfiguration;
import cn.utopiabin.cloud.ai.core.chat.ChatModelProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration(before = ChatCoreAutoConfiguration.class)
@ConditionalOnClass({ChatModel.class, WebClient.class})
@ConditionalOnProperty(prefix = TencentChatProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TencentChatProperties.class)
public class TencentChatAutoConfiguration {

    @Bean("tencentChatApi")
    @ConditionalOnMissingBean(name = "tencentChatApi")
    TencentChatApi tencentChatApi(TencentChatProperties properties, WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper) {
        Assert.hasText(properties.getAppKey(), "bin.ai.providers.tencent.app-key must be configured");
        return new WebClientTencentChatApi(properties, webClientBuilder, objectMapper);
    }

    @Bean("tencentChatModel")
    @ConditionalOnMissingBean(name = "tencentChatModel")
    TencentChatModel tencentChatModel(TencentChatApi chatApi, TencentChatProperties properties) {
        TencentChatProperties.Defaults defaults = properties.getDefaults();
        TencentChatOptions options = new TencentChatOptions();
        options.setAppKey(properties.getAppKey());
        options.setModel(defaults.getModel());
        options.setStreamingThrottle(defaults.getStreamingThrottle());
        options.setIncremental(defaults.getIncremental());
        options.setSearchNetwork(defaults.getSearchNetwork());
        options.setStream("enable");
        options.setWorkflowStatus(defaults.getWorkflowStatus());
        options.setEnableMultiIntent(defaults.getEnableMultiIntent());
        return new TencentChatModel(chatApi, options);
    }

    @Bean("tencentChatModelProvider")
    @ConditionalOnMissingBean(name = "tencentChatModelProvider")
    ChatModelProvider tencentChatModelProvider(TencentChatModel chatModel) {
        return new TencentChatModelProvider(chatModel);
    }
}
