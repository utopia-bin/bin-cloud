package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字典树节点 VO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典树节点")
public class SysDictOptionsTreeVO extends JsonSerializable {

    @Schema(description = "字典项ID")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "字典项名称")
    private String name;

    @Schema(description = "字典项值")
    private String value;

    @Schema(description = "子节点")
    private List<SysDictOptionsTreeVO> children;

    public SysDictOptionsTreeVO(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
