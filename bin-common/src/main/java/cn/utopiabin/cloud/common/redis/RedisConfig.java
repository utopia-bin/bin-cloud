package cn.utopiabin.cloud.common.redis;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 配置属性
 *
 * @since 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisConfig extends JsonSerializable {
    /**
     * 全局 Key 前缀
     */
    private String keyPrefix;
}
