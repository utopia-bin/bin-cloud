package cn.utopiabin.cloud.platform.model.dto.common;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 批量删除参数
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批量删除参数")
public class BatchDeleteDTO extends JsonSerializable {

    @NotEmpty(message = "ID列表不能为空")
    @Size(min = 1, message = "至少选择一条记录")
    @Schema(description = "ID列表", example = "[1, 2, 3]")
    private List<Long> ids;
}
