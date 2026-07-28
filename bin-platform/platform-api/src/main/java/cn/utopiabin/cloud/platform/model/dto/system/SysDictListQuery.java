package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典列表查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典列表查询")
public class SysDictListQuery extends JsonSerializable {

    @Schema(description = "是否启用")
    private Boolean available;
}
