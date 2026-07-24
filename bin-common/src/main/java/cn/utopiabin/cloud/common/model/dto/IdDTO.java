package cn.utopiabin.cloud.common.model.dto;

import cn.utopiabin.cloud.common.json.JsonSerializable;
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
public class IdDTO extends JsonSerializable {

    private Long id;

    public static IdDTO of(Long id) {
        return new IdDTO(id);
    }
}
