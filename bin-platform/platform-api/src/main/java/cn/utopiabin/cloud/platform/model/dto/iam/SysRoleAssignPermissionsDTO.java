package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 角色全量替换权限命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色全量分配权限参数")
public class SysRoleAssignPermissionsDTO extends JsonSerializable {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "待分配权限的角色 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleId;

    @NotNull(message = "权限ID列表不能为空")
    @Schema(description = "替换后的权限 ID 列表；空列表表示清除角色的全部权限", example = "[1, 2]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> permissionIds;

    /** 乐观并发控制版本。 */
    @NotNull(message = "角色版本号不能为空")
    @Schema(description = "客户端读取到的角色版本号，用于乐观并发控制", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
}
