package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统菜单列表查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统菜单列表查询")
public class SysMenuListQuery extends JsonSerializable {

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "是否启用")
    private Boolean available;
}
