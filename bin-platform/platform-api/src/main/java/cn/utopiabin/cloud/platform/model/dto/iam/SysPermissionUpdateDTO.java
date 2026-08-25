package cn.utopiabin.cloud.platform.model.dto.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限资源更新命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限资源编辑参数")
public class SysPermissionUpdateDTO extends SysPermissionCreateDTO {

    @NotNull(message = "权限ID不能为空")
    @Schema(description = "待编辑权限资源 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "版本号不能为空")
    @Schema(description = "客户端读取到的权限版本号，用于乐观并发控制", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
}
