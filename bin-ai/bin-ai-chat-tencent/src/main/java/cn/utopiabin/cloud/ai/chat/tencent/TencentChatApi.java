package cn.utopiabin.cloud.ai.chat.tencent;

import reactor.core.publisher.Flux;

interface TencentChatApi {

    Flux<TencentChatProtocol.ChatResponseChunk> stream(TencentChatProtocol.ChatRequest request);
}
