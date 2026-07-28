package cn.utopiabin.cloud.platform.constant;

import lombok.Getter;

/**
 * 平台错误码枚举
 * <p>
 * 统一管理平台业务错误码，替代 BizException 中的硬编码消息
 *
 * @since 1.0
 */
@Getter
public enum PlatformErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无操作权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DUPLICATE(1002, "用户名已存在"),
    USER_DISABLED(1003, "用户已被禁用"),
    PASSWORD_ERROR(1004, "用户名或密码错误"),
    PASSWORD_EMPTY(1005, "密码不能为空"),
    PASSWORD_WRONG(1006, "原密码错误"),

    ROLE_NOT_FOUND(1101, "角色不存在"),
    ROLE_CODE_DUPLICATE(1102, "角色编码已存在"),

    MENU_NOT_FOUND(1201, "菜单不存在"),
    MENU_HAS_CHILDREN(1202, "存在子级菜单，请先删除子级"),
    MENU_PARENT_SELF(1203, "上级菜单不能是自身"),

    TENANT_NOT_FOUND(1301, "租户不存在"),
    TENANT_CODE_DUPLICATE(1302, "租户编码已存在"),

    DICT_NOT_FOUND(1401, "字典不存在"),
    DICT_DUPLICATE(1402, "字典名称或编码已存在"),
    DICT_OPTION_NOT_FOUND(1403, "字典项不存在"),
    DICT_OPTION_DUPLICATE(1404, "字典项名称或值已存在"),
    DICT_OPTION_HAS_CHILDREN(1405, "存在子级字典项，请先删除子级"),
    DICT_OPTION_PARENT_SELF(1406, "上级字典项不能是自身"),

    PARAM_NOT_FOUND(1501, "系统参数不存在"),
    PARAM_KEY_DUPLICATE(1502, "参数Key已存在"),

    FAIL(500, "操作失败");

    private final int code;
    private final String msg;

    PlatformErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 构建异常消息
     */
    public String message(String detail) {
        return detail != null && !detail.isBlank() ? detail : this.msg;
    }
}
