package cn.utopiabin.cloud.platform.annotation;

/**
 * 操作类型
 *
 * @since 1.0
 */
public enum OperateType {

    /** 新增 */
    CREATE,

    /** 修改 */
    UPDATE,

    /** 删除 */
    DELETE,

    /** 查询/导出 */
    QUERY,

    /** 分配 (角色/菜单/权限) */
    ASSIGN,

    /** 启用/禁用 */
    ENABLE,

    /** 认证 (登录/登出/改密/重置) */
    AUTH,

    /** 其他 */
    OTHER
}
