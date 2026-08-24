package cn.utopiabin.cloud.ai.chat.tencent;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;

public class TencentChatOptions implements ChatOptions {

    @JsonProperty("RequestId")
    private String requestId;
    @JsonProperty("ConversationId")
    private String conversationId;
    @JsonProperty("AppKey")
    private String appKey;
    @JsonProperty("VisitorId")
    private String visitorId;
    @JsonProperty("StreamingThrottle")
    private Integer streamingThrottle;
    @JsonProperty("Incremental")
    private Boolean incremental;
    @JsonProperty("SearchNetwork")
    private String searchNetwork;
    @JsonProperty("ModelName")
    private String model;
    @JsonProperty("Stream")
    private String stream;
    @JsonProperty("WorkflowStatus")
    private String workflowStatus;
    @JsonProperty("EnableMultiIntent")
    private Boolean enableMultiIntent;
    @JsonProperty("GenerateAgain")
    private Boolean generateAgain;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ChatOptions> T copy() {
        TencentChatOptions copy = new TencentChatOptions();
        copy.requestId = this.requestId;
        copy.conversationId = this.conversationId;
        copy.appKey = this.appKey;
        copy.visitorId = this.visitorId;
        copy.streamingThrottle = this.streamingThrottle;
        copy.incremental = this.incremental;
        copy.searchNetwork = this.searchNetwork;
        copy.model = this.model;
        copy.stream = this.stream;
        copy.workflowStatus = this.workflowStatus;
        copy.enableMultiIntent = this.enableMultiIntent;
        copy.generateAgain = this.generateAgain;
        return (T) copy;
    }

    public String getRequestId() { return this.requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getConversationId() { return this.conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getAppKey() { return this.appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getVisitorId() { return this.visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public Integer getStreamingThrottle() { return this.streamingThrottle; }
    public void setStreamingThrottle(Integer streamingThrottle) { this.streamingThrottle = streamingThrottle; }
    public Boolean getIncremental() { return this.incremental; }
    public void setIncremental(Boolean incremental) { this.incremental = incremental; }
    public String getSearchNetwork() { return this.searchNetwork; }
    public void setSearchNetwork(String searchNetwork) { this.searchNetwork = searchNetwork; }

    @Override
    public String getModel() { return this.model; }

    public void setModel(String model) { this.model = model; }
    public String getStream() { return this.stream; }
    public void setStream(String stream) { this.stream = stream; }
    public String getWorkflowStatus() { return this.workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }
    public Boolean getEnableMultiIntent() { return this.enableMultiIntent; }
    public void setEnableMultiIntent(Boolean enableMultiIntent) { this.enableMultiIntent = enableMultiIntent; }
    public Boolean getGenerateAgain() { return this.generateAgain; }
    public void setGenerateAgain(Boolean generateAgain) { this.generateAgain = generateAgain; }

    @Override
    public Double getTemperature() {
        return null;
    }

    @Override
    public Double getTopP() {
        return null;
    }

    @Override
    public Integer getTopK() {
        return null;
    }

    @Override
    public Double getFrequencyPenalty() {
        return null;
    }

    @Override
    public Double getPresencePenalty() {
        return null;
    }

    @Override
    public Integer getMaxTokens() {
        return null;
    }

    @Override
    public List<String> getStopSequences() {
        return null;
    }
}
