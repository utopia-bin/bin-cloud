package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统角色分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统角色分页查询")
public class SysRolePageQuery extends PageQuery {

    @Schema(description = "关键词（角色名称/编码）")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "创建时间起")
    private LocalDateTime startTime;

    @Schema(description = "创建时间止")
    private LocalDateTime endTime;
}
