package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.model.dto.IdDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统用户编辑 DTO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统用户编辑参数")
public class SysUserUpdateDTO extends IdDTO {

    @jakarta.validation.constraints.NotNull(message = "版本号不能为空")
    private Integer expectedVersion;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Schema(description = "用户名（唯一）", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Size(max = 100, message = "密码长度不能超过100个字符")
    @Schema(description = "密码（不填表示不改）", example = "123456")
    private String password;

    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    @Schema(description = "真实姓名", example = "管理员")
    private String realName;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "性别: 0未知 1男 2女", example = "1")
    private Integer gender;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "排序码", example = "10")
    private Integer sort;

    @Schema(description = "备注")
    private String comment;
}
