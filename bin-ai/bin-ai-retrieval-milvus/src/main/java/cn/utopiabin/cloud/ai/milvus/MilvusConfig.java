package cn.utopiabin.cloud.ai.milvus;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Milvus connection properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = MilvusConfig.PREFIX)
@EqualsAndHashCode(callSuper = false)
public class MilvusConfig extends JsonSerializable {

    public static final String PREFIX = "milvus";

    private String uri;
    private String token;
    private String username;
    private String password;
    private String databaseName;
    private long connectTimeoutMs = 10_000L;
    private long rpcDeadlineMs;
    private long keepAliveTimeMs = 10_000L;
    private long keepAliveTimeoutMs = 5_000L;
    private boolean keepAliveWithoutCalls = true;
    private boolean secure;
    private boolean enablePrecheck;
}
