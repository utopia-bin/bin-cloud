package cn.utopiabin.cloud.gateway.config;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

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
@ConfigurationProperties(prefix = "gateway")
public class GatewayConfig extends JsonSerializable {
    /**
     * JWT 签名密钥 (至少 256 bit, 32 字符以上)
     */
    private String jwtSecret = "";

    /**
     * JWT Token 过期时间 (秒), 默认 7200 = 2 小时
     */
    private long jwtExpiration = 7200;

    /**
     * 鉴权白名单路径 (Ant 风格)
     */
    private List<String> whitePaths = List.of();

    /**
     * 限流: 每秒令牌生成数
     */
    private int rateLimitReplenishRate = 10;

    /**
     * 限流: 令牌桶容量
     */
    private int rateLimitBurstCapacity = 20;

    /**
     * 限流: 每个 IP 每分钟最大请求数
     */
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
    private double canaryRatio = 0.1;

    /**
     * Token 黑名单: 是否启用 (基于 Redis, 用于主动注销/踢人)
     */
    private boolean tokenBlacklistEnabled = false;

    /**
     * 请求体大小上限 (字节), 默认 10MB
     */
    private long maxRequestSize = 10L * 1024 * 1024;
}
