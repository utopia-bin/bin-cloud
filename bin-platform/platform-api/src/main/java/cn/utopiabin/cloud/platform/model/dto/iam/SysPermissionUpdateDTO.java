package cn.utopiabin.cloud.platform.model.dto.iam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限资源更新命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysPermissionUpdateDTO extends SysPermissionCreateDTO {

    @NotNull(message = "权限ID不能为空")
    private Long id;

    @NotNull(message = "版本号不能为空")
    private Integer expectedVersion;
}
