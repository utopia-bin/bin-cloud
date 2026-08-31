package cn.utopiabin.cloud.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 首次部署配置；密码只用于首次创建，不参与日志输出或已有账号更新。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.bootstrap")
public class PlatformBootstrapProperties {
    private boolean enabled;
    private String tenantCode = "default";
    private String tenantName = "默认租户";
    private String username = "admin";
    private String password;
}
