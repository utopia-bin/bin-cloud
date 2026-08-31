package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用实例角色及权限分配参数")
public class ApplicationRoleDTO extends JsonSerializable {
    @Schema(description = "角色ID，新增留空")
    private Long id;
    @Schema(description = "编辑必填的乐观锁版本")
    private Integer expectedVersion;
    @Schema(description = "角色所属开通实例")
    @NotNull @Positive
    private Long tenantApplicationId;
    @Schema(description = "应用实例内唯一角色编码")
    @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,49}")
    private String code;
    @Schema(description = "角色名称")
    @NotBlank @Size(max = 50)
    private String name;
    @Schema(description = "1全部、4仅本人，不提供不存在的部门模型")
    @Min(1) @Max(4)
    private int dataScope = 4;
    @Schema(description = "是否启用角色")
    private boolean available = true;
    @Schema(description = "显示顺序")
    @Min(0)
    private int sort = 10;
    @Schema(description = "只能包含此应用发布的权限ID")
    @NotNull @Size(max = 200)
    private List<@NotNull @Positive Long> permissionIds = new java.util.ArrayList<>();
}
