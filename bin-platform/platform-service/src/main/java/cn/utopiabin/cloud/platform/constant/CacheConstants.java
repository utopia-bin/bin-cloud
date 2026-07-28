package cn.utopiabin.cloud.platform.constant;

/**
 * 缓存 Key 常量
 * <p>
 * 集中管理 Spring Cache 缓存名称与 Key 模板
 *
 * @since 1.0
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /** 全量菜单树缓存名 */
    public static final String MENU_TREE = "menu:tree";

    /** 用户权限缓存名前缀 */
    public static final String USER_PERM = "user:perm";

    /**
     * 构建用户权限缓存 Key
     *
     * @param userId 用户 ID
     * @return 缓存 Key
     */
    public static String userPermKey(Long userId) {
        return USER_PERM + ":" + userId;
    }
}
