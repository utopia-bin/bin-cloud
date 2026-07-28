package cn.utopiabin.cloud.common.context;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 用户上下文持有者 —— 基于 {@link ThreadLocal}
 * <p>
 * 提供线程级的用户信息存储与读取, 在 Servlet 请求生命周期内由
 * {@link UserContextFilter} 自动设置/清理, 并通过 Dubbo Filter 实现跨服务 RPC 透传。
 * <p>
 * 使用普通 ThreadLocal 而非 InheritableThreadLocal, 因为 InheritableThreadLocal
 * 在线程池场景下仅在线程创建时拷贝父值, 线程复用时不会重新继承,
 * 会导致上一个请求的上下文泄漏到后续请求 (串上下文)。
 * 如需线程池场景下父子线程自动传递, 可升级为阿里的 TransmittableThreadLocal。
 * <p>
 * 典型用法:
 * <pre>{@code
 * // 任意位置获取当前用户
 * String userId = UserContextHolder.getUserId();
 * String tenantId = UserContextHolder.getTenantId();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    // ==================== 基础操作 ====================

    /**
     * 设置当前线程的用户上下文
     */
    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程的用户上下文 (可能为 null)
     */
    public static UserContext get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前线程的用户上下文, 包装为 Optional
     */
    public static Optional<UserContext> getOptional() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * 清除当前线程的用户上下文 (请求结束后必须调用)
     */
    public static void clear() {
        CONTEXT.remove();
    }

    // ==================== 便捷取值方法 ====================

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID 或 null
     */
    public static String getUserId() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名或 null
     */
    public static String getUsername() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUsername() : null;
    }

    /**
     * 获取当前租户 ID
     *
     * @return 租户 ID 或 null
     */
    public static String getTenantId() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    /**
     * 获取当前用户角色列表
     *
     * @return 角色列表 (可能为空列表, 不会返回 null)
     */
    public static List<String> getRoles() {
        UserContext ctx = CONTEXT.get();
        if (ctx == null || ctx.getRoles() == null) {
            return Collections.emptyList();
        }
        return ctx.getRoles();
    }

    /**
     * 判断当前请求是否已设置用户上下文
     */
    public static boolean isPresent() {
        UserContext ctx = CONTEXT.get();
        return ctx != null && ctx.isValid();
    }
}
