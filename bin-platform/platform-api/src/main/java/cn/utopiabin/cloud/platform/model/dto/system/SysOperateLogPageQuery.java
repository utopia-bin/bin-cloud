package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志分页查询")
public class SysOperateLogPageQuery extends PageQuery {

    @Schema(description = "业务模块 (精确匹配)")
    private String module;

    @Schema(description = "操作人用户名 (精确匹配)")
    private String operateUsername;

    @Schema(description = "是否成功 (true/false)")
    private Boolean success;

    @Schema(description = "搜索关键字（匹配模块/动作/操作人）")
    private String keyword;
}
