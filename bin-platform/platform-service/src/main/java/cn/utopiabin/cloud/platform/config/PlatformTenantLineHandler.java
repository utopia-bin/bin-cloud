package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.tenant.TenantIgnoreContext;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * 平台多租户 SQL 隔离处理器
 * <p>
 * 由 MyBatis-Plus TenantLineInnerInterceptor 回调，为所有业务 SQL 自动追加
 * {@code tenant_id = 当前租户} 条件，实现行级数据隔离:
 * <ul>
 *   <li>租户 ID 取自 {@link UserContextHolder} (网关 JWT → 请求头 → Dubbo Filter 透传)</li>
 *   <li>无租户上下文时返回 0 (默认安全: 查不到任何业务数据)</li>
 *   <li>忽略清单内的表 (租户表/全局菜单/关联表/日志表) 不追加条件</li>
 *   <li>{@link TenantIgnoreContext} 开启时 (登录等认证场景) 整体跳过</li>
 *   <li>超级管理员默认跳过隔离 (跨租户管理)</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformTenantLineHandler implements TenantLineHandler {

    /** 无租户上下文时的兜底租户 ID (业务数据不会存在 tenant_id=0) */
    private static final long FALLBACK_TENANT_ID = 0L;

    private final TenantProperties tenantProperties;

    @Override
    public Expression getTenantId() {
        String tenantId = UserContextHolder.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            // 无上下文 (定时任务/内部调用未透传) → 返回兜底值, 防止跨租户读取
            log.debug("租户上下文为空, 使用兜底租户 ID: {}", FALLBACK_TENANT_ID);
            return new LongValue(FALLBACK_TENANT_ID);
        }
        try {
            return new LongValue(Long.parseLong(tenantId.trim()));
        } catch (NumberFormatException e) {
            log.warn("租户 ID 非法, 使用兜底租户 ID: value={}", tenantId);
            return new LongValue(FALLBACK_TENANT_ID);
        }
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 1. 功能总开关
        if (!tenantProperties.isEnabled()) {
            return true;
        }
        // 2. 忽略清单 (无 tenant_id 列或全局共享的表)
        if (tenantProperties.getIgnoreTables().contains(tableName)) {
            return true;
        }
        // 3. 显式忽略 (@TenantIgnore 认证/系统场景)
        if (TenantIgnoreContext.isIgnore()) {
            return true;
        }
        // 不根据 JWT 中的角色跳过隔离。跨租户平台操作必须通过显式、可审计的应用服务完成。
        return false;
    }
}
