package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项新增 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项新增参数")
public class SysDictOptionsCreateDTO extends JsonSerializable {

    @Schema(description = "父级字典项ID（为空则取0）", example = "0")
    private Long parentId;

    @NotNull(message = "所属字典ID不能为空")
    @Schema(description = "所属字典ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dictId;

    @NotBlank(message = "字典项名称不能为空")
    @Size(max = 50, message = "字典项名称长度不能超过50")
    @Schema(description = "字典项名称", example = "男", requiredMode = Schema.RequiredMode.REQUIRED)
    private String optionName;

    @NotBlank(message = "字典项值不能为空")
    @Size(max = 100, message = "字典项值长度不能超过100")
    @Schema(description = "字典项值", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String optionValue;

    @Size(max = 200, message = "字典项描述长度不能超过200")
    @Schema(description = "字典项描述")
    private String optionComment;

    @Schema(description = "排序码", example = "10")
    private Integer sort;
}
