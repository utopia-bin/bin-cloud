package cn.utopiabin.cloud.platform.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "短信验证码场景：REGISTER 注册、LOGIN 登录、RESET_PASSWORD 重置密码")
public enum SmsScene {
    REGISTER,
    LOGIN,
    RESET_PASSWORD
}
