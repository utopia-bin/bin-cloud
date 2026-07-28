package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色列表查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统角色列表查询")
public class SysRoleListQuery extends JsonSerializable {

    @Schema(description = "是否启用")
    private Boolean available;
}
