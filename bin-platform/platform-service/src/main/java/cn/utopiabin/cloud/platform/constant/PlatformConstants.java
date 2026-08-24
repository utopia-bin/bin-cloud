package cn.utopiabin.cloud.platform.constant;

/**
 * 平台级常量
 * <p>
 * 集中管理平台内置的特殊编码与用户名，用于内置数据保护。
 *
 * @since 1.0
 */
public final class PlatformConstants {

    private PlatformConstants() {
    }

    /** 内置超级管理员角色编码 (该角色不可删除/禁用/修改编码) */
    public static final String SUPER_ADMIN_ROLE_CODE = "super_admin";

    /** 内置管理员用户名 (该账号不可删除/禁用) */
    public static final String BUILT_IN_ADMIN_USERNAME = "admin";
}
