package cn.utopiabin.cloud.ai.chat.tencent;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class TencentChatException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String providerCode;
    private final String providerMessage;
    private final String requestId;

    private TencentChatException(String message, HttpStatusCode statusCode, String providerCode,
                                 String providerMessage, String requestId) {
        super(message);
        this.statusCode = statusCode;
        this.providerCode = providerCode;
        this.providerMessage = providerMessage;
        this.requestId = requestId;
    }

    static TencentChatException http(HttpStatusCode statusCode, String responseBody) {
        return new TencentChatException("Tencent chat API returned HTTP " + statusCode.value(),
                statusCode, null, responseBody, null);
    }

    static TencentChatException event(String code, String providerMessage, String requestId) {
        String suffix = code == null ? "" : " (code=" + code + ")";
        return new TencentChatException("Tencent chat API returned an error event" + suffix,
                null, code, providerMessage, requestId);
    }

}
