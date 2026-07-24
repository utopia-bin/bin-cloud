package cn.utopiabin.cloud.common.es;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置
 *
 * @since 1.0
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "es")
@EqualsAndHashCode(callSuper = false)
public class ESConfig extends JsonSerializable {
    /**
     * 地址
     */
    private String apiUrl;

    /**
     * API 密钥
     */
    private String apiKey;
}
