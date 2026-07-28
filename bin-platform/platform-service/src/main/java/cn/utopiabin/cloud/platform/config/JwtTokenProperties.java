package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * <p>
 * 对应 Nacos 配置:
 * <pre>
 * gateway:
 *   jwt-secret: "xxx"
 *   jwt-expiration: 7200
 *   token-blacklist-enabled: false
 * </pre>
 *
 * @since 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway")
public class JwtTokenProperties extends JsonSerializable {

    /**
     * JWT 签名密钥 (与 gateway 共用)
     */
    private String jwtSecret = "changeme-this-is-a-default-jwt-secret-key-32chars";

    /**
     * JWT 过期时长 (秒)
     */
    private long jwtExpiration = 7200;

    /**
     * 是否启用 Token 黑名单
     */
    private boolean tokenBlacklistEnabled = false;
}
