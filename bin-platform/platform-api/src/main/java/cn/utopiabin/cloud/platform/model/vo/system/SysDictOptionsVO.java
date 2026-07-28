package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项")
public class SysDictOptionsVO extends BaseVO {

    @Schema(description = "父级字典项ID")
    private Long parentId;

    @Schema(description = "所属字典ID")
    private Long dictId;

    @Schema(description = "字典项名称")
    private String optionName;

    @Schema(description = "字典项值")
    private String optionValue;

    @Schema(description = "字典项描述")
    private String optionComment;

    @Schema(description = "排序码")
    private Integer sort;
}
