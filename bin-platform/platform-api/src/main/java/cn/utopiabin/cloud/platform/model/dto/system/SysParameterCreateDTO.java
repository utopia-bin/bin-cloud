package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数新增 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统参数新增参数")
public class SysParameterCreateDTO extends JsonSerializable {

    @NotBlank(message = "参数键不能为空")
    @Size(max = 100, message = "参数键长度不能超过100")
    @Schema(description = "参数键", example = "system.name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramKey;

    @Size(max = 500, message = "参数值长度不能超过500")
    @Schema(description = "参数值", example = "平台基座")
    private String paramValue;

    @Size(max = 200, message = "参数描述长度不能超过200")
    @Schema(description = "参数描述")
    private String paramComment;

    @Schema(description = "排序码", example = "10")
    private Integer sort;
}
