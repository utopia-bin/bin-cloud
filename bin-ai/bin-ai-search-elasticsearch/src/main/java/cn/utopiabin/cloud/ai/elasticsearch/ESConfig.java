package cn.utopiabin.cloud.ai.elasticsearch;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Elasticsearch 配置
 *
 * @since 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = ESConfig.PREFIX)
@EqualsAndHashCode(callSuper = false)
public class ESConfig extends JsonSerializable {

    public static final String PREFIX = "es";
    /**
     * 地址
     */
    private String apiUrl;

    /**
     * API 密钥
     */
    private String apiKey;
}
