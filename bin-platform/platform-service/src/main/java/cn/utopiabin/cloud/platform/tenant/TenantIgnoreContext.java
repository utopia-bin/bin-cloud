package cn.utopiabin.cloud.platform.tenant;

/**
 * 租户忽略上下文 —— 基于 ThreadLocal 的 SQL 隔离开关
 * <p>
 * 由 {@code @TenantIgnore} 注解的切面自动设置/清除，
 * 供 {@code TenantLineHandler} 判断当前调用链是否跳过租户过滤。
 * <p>
 * 使用计数器支持嵌套调用 (内层方法返回时不提前关闭外层的忽略状态)。
 *
 * @since 1.0
 */
public final class TenantIgnoreContext {

    private static final ThreadLocal<Integer> IGNORE_DEPTH = new ThreadLocal<>();

    private TenantIgnoreContext() {
    }

    /**
     * 开启忽略 (进入 @TenantIgnore 方法)
     */
    public static void enable() {
        Integer depth = IGNORE_DEPTH.get();
        IGNORE_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    /**
     * 关闭忽略 (退出 @TenantIgnore 方法，嵌套时仅减少计数)
     */
    public static void disable() {
        Integer depth = IGNORE_DEPTH.get();
        if (depth == null) {
            return;
        }
        if (depth <= 1) {
            IGNORE_DEPTH.remove();
        } else {
            IGNORE_DEPTH.set(depth - 1);
        }
    }

    /**
     * 当前调用链是否忽略租户过滤
     */
    public static boolean isIgnore() {
        Integer depth = IGNORE_DEPTH.get();
        return depth != null && depth > 0;
    }

    /**
     * 强制清理 (防御性，防止异常路径泄漏)
     */
    public static void clear() {
        IGNORE_DEPTH.remove();
    }
}
