package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项分页查询")
public class SysDictOptionsPageQuery extends PageQuery {

    @NotNull(message = "所属字典ID不能为空")
    @Schema(description = "所属字典ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dictId;
}
