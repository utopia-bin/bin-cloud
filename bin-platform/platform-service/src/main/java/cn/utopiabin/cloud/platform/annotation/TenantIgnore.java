package cn.utopiabin.cloud.platform.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略多租户 SQL 隔离
 * <p>
 * 标注在 Service 方法上，方法执行期间的所有 SQL 将不追加 tenant_id 条件。
 * <p>
 * 适用场景:
 * <ul>
 *   <li>登录认证 —— 用户名全局唯一，需跨租户查询用户</li>
 *   <li>系统级内部任务 —— 无租户上下文的定时任务/初始化逻辑</li>
 * </ul>
 * <b>注意: 仅用于确需跨租户访问的安全场景，业务查询禁止使用。</b>
 *
 * @since 1.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantIgnore {
}
