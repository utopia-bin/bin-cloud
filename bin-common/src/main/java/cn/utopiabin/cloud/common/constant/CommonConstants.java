package cn.utopiabin.cloud.common.constant;

/**
 * 全局通用常量
 * <p>
 * 定义系统级别的约定常量, 供所有模块 (gateway、api、platform 等) 统一引用,
 * 避免各模块之间通过 magic string 通信。
 *
 * @since 1.0.0
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    // ==================== 请求头 Key ====================

    /** 全链路追踪 ID */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 用户 ID (网关鉴权后注入) */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 用户名 (网关鉴权后注入) */
    public static final String HEADER_USER_NAME = "X-User-Name";

    /** 用户角色 (网关鉴权后注入, 逗号分隔) */
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    /** 租户 ID (网关鉴权后注入, 多租户数据隔离) */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /** 灰度版本标记 */
    public static final String HEADER_CANARY = "X-Canary";

    /** 灰度版本号 */
    public static final String HEADER_VERSION = "X-Version";

    /** 原始 JWT Token (网关鉴权后透传, 用于主动注销黑名单) */
    public static final String HEADER_TOKEN = "X-Token";

    // ==================== Token ====================

    /** Token 查询参数名 */
    public static final String TOKEN_PARAM = "token";

    /** Authorization 请求头 Bearer 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Bearer 前缀长度 */
    public static final int BEARER_PREFIX_LENGTH = 7;
}
