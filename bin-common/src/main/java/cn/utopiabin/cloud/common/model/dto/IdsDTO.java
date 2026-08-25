package cn.utopiabin.cloud.common.model.dto;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 多 ID 请求 DTO (批量操作)
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量资源 ID 请求参数")
public class IdsDTO extends JsonSerializable {

    @Schema(description = "目标资源 ID 列表", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    public List<Long> safeIds() {
        return ids != null ? ids : List.of();
    }

    public static IdsDTO of(List<Long> ids) {
        return new IdsDTO(ids);
    }
}
