package cn.utopiabin.cloud.gateway.config;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Gateway 全局配置属性 (支持 Nacos 动态刷新)
 *
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Component
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayConfig extends JsonSerializable {
    /**
     * JWT 签名密钥 (至少 256 bit, 32 字符以上)
     */
    @NotBlank
    @Size(min = 32)
    private String jwtSecret;

    /**
     * JWT Token 过期时间 (秒), 默认 7200 = 2 小时
     */
    @Min(60)
    private long jwtExpiration = 7200;

    /**
     * 鉴权白名单路径 (Ant 风格)
     */
    private List<String> whitePaths = List.of();

    /**
     * 限流: 每秒令牌生成数
     */
    @Min(1)
    private int rateLimitReplenishRate = 10;

    /**
     * 限流: 令牌桶容量
     */
    @Min(1)
    private int rateLimitBurstCapacity = 20;

    /**
     * 限流: 每个 IP 每分钟最大请求数
     */
    @Min(1)
    private int rateLimitPerMinute = 100;

    /**
     * 限流: 是否启用
     */
    private boolean rateLimitEnabled = true;

    /**
     * 灰度: 是否启用金丝雀路由
     */
    private boolean canaryEnabled = false;

    /**
     * 灰度: 金丝雀版本流量比例 (0.0 ~ 1.0)
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double canaryRatio = 0.1;

    /** 灰度请求没有匹配实例时是否允许回退到稳定实例。 */
    private boolean canaryFallbackEnabled = false;

    /** 是否允许外部请求头直接指定灰度版本；默认关闭，避免客户端绕过灰度策略。 */
    private boolean canaryHeaderEnabled = false;

    /**
     * Token 黑名单: 是否启用 (基于 Redis, 用于主动注销/踢人)
     */
    private boolean tokenBlacklistEnabled = false;

    /** 黑名单启用后 Redis 不可用时是否拒绝请求，默认安全关闭。 */
    private boolean tokenBlacklistFailClosed = true;

    /**
     * 请求体大小上限 (字节), 默认 10MB
     */
    @Min(1)
    private long maxRequestSize = 10L * 1024 * 1024;

    /** 允许使用查询参数 token 的 WebSocket 路径。 */
    private List<String> websocketTokenPaths = List.of("/ws/**", "/websocket/**");

    /** 允许跨域访问的来源模式，多个值使用逗号分隔。 */
    @NotBlank
    private String allowedOriginPatterns = "http://localhost:*,http://127.0.0.1:*";

    /** 可被信任并允许提供转发客户端地址的反向代理 IP。 */
    private List<String> trustedProxyAddresses = List.of("127.0.0.1", "0:0:0:0:0:0:0:1");
}
