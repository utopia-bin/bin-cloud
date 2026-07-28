package cn.utopiabin.cloud.platform.model.dto.tenant;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户列表查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户列表查询")
public class TenantListQuery extends JsonSerializable {

    @Schema(description = "是否启用")
    private Boolean available;
}
