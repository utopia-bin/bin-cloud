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
    ACCOUNT_LOCKED(1007, "失败次数过多，账号已锁定，请稍后再试"),
    PASSWORD_WEAK(1008, "密码强度不足，需包含大小写字母、数字，至少8位"),
    PHONE_DUPLICATE(1009, "手机号已注册"),
    PHONE_NOT_REGISTERED(1010, "手机号未注册"),
    CANNOT_OPERATE_SELF(1011, "不能对当前登录账号执行此操作"),
    BUILT_IN_PROTECTED(1012, "内置数据不允许此操作"),

    ROLE_NOT_FOUND(1101, "角色不存在"),
    ROLE_CODE_DUPLICATE(1102, "角色编码已存在"),
    ROLE_VERSION_CONFLICT(1103, "角色已被其他操作修改，请刷新后重试"),

    PERMISSION_NOT_FOUND(1151, "权限资源不存在"),
    PERMISSION_CODE_DUPLICATE(1152, "权限编码已存在"),
    PERMISSION_IN_USE(1153, "权限正在被角色或菜单使用，不能删除"),
    PERMISSION_VERSION_CONFLICT(1154, "权限资源已被其他操作修改，请刷新后重试"),

    MENU_NOT_FOUND(1201, "菜单不存在"),
    MENU_HAS_CHILDREN(1202, "存在子级菜单，请先删除子级"),
    MENU_PARENT_SELF(1203, "上级菜单不能是自身"),

    TENANT_RELATION_VIOLATION(1251, "关联资源不属于当前租户"),

    TENANT_NOT_FOUND(1301, "租户不存在"),
    TENANT_CODE_DUPLICATE(1302, "租户编码已存在"),
    TENANT_DISABLED(1303, "所属租户已被禁用，请联系管理员"),
    TENANT_EXPIRED(1304, "所属租户已过期，请联系管理员"),

    SMS_CODE_ERROR(1351, "短信验证码错误或已过期"),
    SMS_SEND_TOO_FREQUENT(1352, "短信发送过于频繁，请稍后再试"),
    SMS_PROVIDER_UNAVAILABLE(1353, "短信服务暂不可用"),
    SMS_SEND_FAILED(1354, "短信发送失败"),

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
