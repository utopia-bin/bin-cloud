package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户分配角色 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分配角色")
public class SysUserAssignRolesDTO extends JsonSerializable {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "角色ID列表不能为空")
    @Schema(description = "角色ID列表（空列表表示清除所有角色）", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> roleIds;
}
