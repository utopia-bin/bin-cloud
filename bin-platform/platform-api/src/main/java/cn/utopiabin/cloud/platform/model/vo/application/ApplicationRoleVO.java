package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用实例角色")
public class ApplicationRoleVO extends JsonSerializable {
    @Schema(description = "角色ID")
    private Long id;
    @Schema(description = "角色版本")
    private Integer version;
    @Schema(description = "所属实例ID")
    private Long tenantApplicationId;
    @Schema(description = "角色名称")
    private String name;
    @Schema(description = "实例内角色编码")
    private String code;
    @Schema(description = "1全部、4本人")
    private Integer dataScope;
    @Schema(description = "是否启用")
    private boolean available;
    @Schema(description = "排序")
    private int sort;
    @Schema(description = "应用权限ID")
    private List<Long> permissionIds;
}
