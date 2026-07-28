package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统参数分页查询")
public class SysParameterPageQuery extends PageQuery {

    @Schema(description = "搜索关键字（匹配参数键/参数值/参数描述）")
    private String keyword;
}
