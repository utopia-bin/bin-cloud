package cn.utopiabin.cloud.common.model.dto;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 单 ID 请求 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单个资源 ID 请求参数")
public class IdDTO extends JsonSerializable {

    @Schema(description = "目标资源 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    public static IdDTO of(Long id) {
        return new IdDTO(id);
    }
}
