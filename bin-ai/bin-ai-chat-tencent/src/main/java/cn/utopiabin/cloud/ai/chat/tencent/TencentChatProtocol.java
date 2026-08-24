package cn.utopiabin.cloud.ai.chat.tencent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

final class TencentChatProtocol {

    static final String EVENT_TEXT_DELTA = "text.delta";
    static final String EVENT_TEXT_REPLACE = "text.replace";
    static final String EVENT_RESPONSE_COMPLETED = "response.completed";
    static final String EVENT_ERROR = "error";

    private TencentChatProtocol() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatRequest(
            @JsonProperty("RequestId") String requestId,
            @JsonProperty("ConversationId") String conversationId,
            @JsonProperty("AppKey") String appKey,
            @JsonProperty("VisitorId") String visitorId,
            @JsonProperty("Contents") List<Content> contents,
            @JsonProperty("StreamingThrottle") Integer streamingThrottle,
            @JsonProperty("SystemRole") String systemRole,
            @JsonProperty("Incremental") Boolean incremental,
            @JsonProperty("SearchNetwork") String searchNetwork,
            @JsonProperty("ModelName") String modelName,
            @JsonProperty("Stream") String stream,
            @JsonProperty("WorkflowStatus") String workflowStatus,
            @JsonProperty("EnableMultiIntent") Boolean enableMultiIntent,
            @JsonProperty("GenerateAgain") Boolean generateAgain) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Content(
            @JsonProperty("Type") String type,
            @JsonProperty("Text") String text,
            @JsonProperty("Image") Image image,
            @JsonProperty("CustomVariables") Map<String, String> customVariables,
            @JsonProperty("WidgetAction") WidgetAction widgetAction) {

        static Content text(String text) {
            return new Content("text", text, null, null, null);
        }

        static Content image(String url) {
            return new Content("image", null, new Image(url), null, null);
        }

        static Content customVariables(Map<String, String> variables) {
            return new Content("custom_variables", null, null, variables, null);
        }

        static Content widgetAction(WidgetAction action) {
            return new Content("widget_action", null, null, null, action);
        }
    }

    record Image(@JsonProperty("Url") String url) {
    }

    record WidgetAction(
            @JsonProperty("WidgetId") String widgetId,
            @JsonProperty("WidgetRunId") String widgetRunId,
            @JsonProperty("ActionType") String actionType,
            @JsonProperty("Payload") String payload) {
    }

    record ChatResponseChunk(String event, Map<String, Object> data) {
    }
}
