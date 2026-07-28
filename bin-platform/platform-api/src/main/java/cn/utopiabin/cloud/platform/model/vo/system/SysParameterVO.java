package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统参数")
public class SysParameterVO extends BaseVO {

    @Schema(description = "参数键")
    private String paramKey;

    @Schema(description = "参数值")
    private String paramValue;

    @Schema(description = "参数描述")
    private String paramComment;

    @Schema(description = "排序码")
    private Integer sort;
}
