package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色新增 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统角色新增参数")
public class SysRoleCreateDTO extends JsonSerializable {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    @Schema(description = "角色名称", example = "管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    @Schema(description = "角色编码（唯一）", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "数据权限范围: 1租户内全部 4仅本人（业务数据范围预留，管理接口按功能权限和租户隔离）", example = "1")
    private Integer dataScope;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "排序码", example = "10")
    private Integer sort;

    @Schema(description = "备注")
    private String comment;
}
