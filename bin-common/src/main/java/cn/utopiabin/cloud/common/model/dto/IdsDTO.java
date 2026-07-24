package cn.utopiabin.cloud.common.model.dto;

import cn.utopiabin.cloud.common.json.JsonSerializable;
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
public class IdsDTO extends JsonSerializable {

    private List<Long> ids;

    public List<Long> safeIds() {
        return ids != null ? ids : List.of();
    }

    public static IdsDTO of(List<Long> ids) {
        return new IdsDTO(ids);
    }
}
