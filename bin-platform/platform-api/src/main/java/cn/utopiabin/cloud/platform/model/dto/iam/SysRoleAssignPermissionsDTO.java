package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 角色全量替换权限命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleAssignPermissionsDTO extends JsonSerializable {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotNull(message = "权限ID列表不能为空")
    private List<Long> permissionIds;

    /** 乐观并发控制版本。 */
    @NotNull(message = "角色版本号不能为空")
    private Integer expectedVersion;
}
