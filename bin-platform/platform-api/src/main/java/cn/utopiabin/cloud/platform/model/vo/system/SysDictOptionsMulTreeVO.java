package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字典树 VO（字典编码 + 对应树形数据）
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "多字典组合树")
public class SysDictOptionsMulTreeVO extends JsonSerializable {

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "树形数据")
    private List<SysDictOptionsTreeVO> data;
}
