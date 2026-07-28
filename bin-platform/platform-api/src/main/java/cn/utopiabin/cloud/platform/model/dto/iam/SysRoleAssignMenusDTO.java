package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色分配菜单 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色分配菜单")
public class SysRoleAssignMenusDTO extends JsonSerializable {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleId;

    @NotNull(message = "菜单ID列表不能为空")
    @Schema(description = "菜单ID列表（空列表表示清除所有菜单）", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> menuIds;
}
