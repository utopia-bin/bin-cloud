package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统字典新增 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典新增参数")
public class SysDictCreateDTO extends JsonSerializable {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 50, message = "字典名称长度不能超过50")
    @Schema(description = "字典名称", example = "性别", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "字典编码不能为空")
    @Size(max = 50, message = "字典编码长度不能超过50")
    @Schema(description = "字典编码（唯一）", example = "gender", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Size(max = 200, message = "字典描述长度不能超过200")
    @Schema(description = "字典描述")
    private String comment;

    @Schema(description = "排序码", example = "10")
    private Integer sort;

    @Schema(description = "是否启用", example = "true")
    private Boolean available;
}
