package cn.utopiabin.cloud.ai.chat.tencent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(TencentChatProperties.PREFIX)
public class TencentChatProperties {

    public static final String PREFIX = "bin.ai.providers.tencent";
    public static final String DEFAULT_BASE_URL = "https://wss.lke.cloud.tencent.com";
    public static final String DEFAULT_CHAT_PATH = "/adp/v2/chat";

    private boolean enabled;
    private String baseUrl = DEFAULT_BASE_URL;
    private String chatPath = DEFAULT_CHAT_PATH;
    private String appKey;
    private DataSize maxInMemorySize = DataSize.ofMegabytes(10);
    private final Defaults defaults = new Defaults();

    public static class Defaults {

        private String model;
        private Integer streamingThrottle;
        private Boolean incremental = true;
        private String searchNetwork;
        private String workflowStatus;
        private Boolean enableMultiIntent;

        public String getModel() {
            return this.model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getStreamingThrottle() {
            return this.streamingThrottle;
        }

        public void setStreamingThrottle(Integer streamingThrottle) {
            this.streamingThrottle = streamingThrottle;
        }

        public Boolean getIncremental() {
            return this.incremental;
        }

        public void setIncremental(Boolean incremental) {
            this.incremental = incremental;
        }

        public String getSearchNetwork() {
            return this.searchNetwork;
        }

        public void setSearchNetwork(String searchNetwork) {
            this.searchNetwork = searchNetwork;
        }

        public String getWorkflowStatus() {
            return this.workflowStatus;
        }

        public void setWorkflowStatus(String workflowStatus) {
            this.workflowStatus = workflowStatus;
        }

        public Boolean getEnableMultiIntent() {
            return this.enableMultiIntent;
        }

        public void setEnableMultiIntent(Boolean enableMultiIntent) {
            this.enableMultiIntent = enableMultiIntent;
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChatPath() {
        return this.chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public DataSize getMaxInMemorySize() {
        return this.maxInMemorySize;
    }

    public void setMaxInMemorySize(DataSize maxInMemorySize) {
        this.maxInMemorySize = maxInMemorySize;
    }

    public Defaults getDefaults() {
        return this.defaults;
    }
}
