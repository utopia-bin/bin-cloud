package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项 VO（精简字段，用于缓存和级联展示）
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项缓存VO")
public class SysDictOptionsItemVO extends JsonSerializable {

    @Schema(description = "字典项ID")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "字典项名称")
    private String optionName;

    @Schema(description = "字典项值")
    private String optionValue;
}
