package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统用户分页查询")
public class SysUserPageQuery extends PageQuery {

    @Schema(description = "关键词（用户名/真实姓名/手机号）")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "创建时间起")
    private LocalDateTime startTime;

    @Schema(description = "创建时间止")
    private LocalDateTime endTime;
}
