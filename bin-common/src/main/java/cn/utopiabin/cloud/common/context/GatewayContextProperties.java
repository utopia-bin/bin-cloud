package cn.utopiabin.cloud.common.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.context")
public class GatewayContextProperties {

    /** 网关与下游 API 共享的上下文签名密钥，生产环境必须通过环境变量或配置中心提供。 */
    private String signingSecret = "";

    /** 允许的签名时间偏差，用于限制捕获请求头后的重放窗口。 */
    private Duration signatureMaxAge = Duration.ofSeconds(30);
}
