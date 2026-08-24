package cn.utopiabin.cloud.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 多租户配置
 * <p>
 * 对应配置:
 * <pre>
 * platform:
 *   tenant:
 *     enabled: true
 *     ignore-tables: sys_tenant,sys_menu,...
 * </pre>
 *
 * @since 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform.tenant")
public class TenantProperties {

    /**
     * 是否启用多租户 SQL 隔离
     */
    private boolean enabled = true;

    /**
     * 忽略租户过滤的表 (无 tenant_id 列或全局共享的表)
     */
    private Set<String> ignoreTables = Set.of(
            "sys_tenant",        // 租户表本身
            "sys_menu",          // 菜单全局共享
            "sys_permission",    // 权限资源目录全局共享
            "sys_operate_log"    // 操作日志 (审计需跨租户查询)
    );
}
