package cn.utopiabin.cloud.platform.model.vo.iam;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统用户（不含密码）")
public class SysUserVO extends BaseVO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别: 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "排序码")
    private Integer sort;

    @Schema(description = "备注")
    private String comment;
}
