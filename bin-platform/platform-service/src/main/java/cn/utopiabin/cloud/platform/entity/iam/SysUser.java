package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统用户
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser extends BaseEntity {

    /**
     * 用户名 (唯一)
     */
    @Schema(description = "用户名（唯一）")
    private String username;

    /**
     * 密码 (BCrypt)
     */
    @Schema(description = "密码（BCrypt加密）")
    private String password;

    @Schema(description = "凭证版本，修改密码时递增以失效旧会话")
    private Integer credentialVersion = 0;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 性别: 0未知 1男 2女
     */
    @Schema(description = "性别: 0未知 1男 2女")
    private Integer gender;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean available;

    /**
     * 排序码
     */
    @Schema(description = "排序码")
    private Integer sort;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String comment;
}
