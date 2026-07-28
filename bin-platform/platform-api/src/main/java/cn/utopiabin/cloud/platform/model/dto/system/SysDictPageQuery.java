package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 字典分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典分页查询")
public class SysDictPageQuery extends PageQuery {

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "创建时间起")
    private LocalDateTime startTime;

    @Schema(description = "创建时间止")
    private LocalDateTime endTime;
}
