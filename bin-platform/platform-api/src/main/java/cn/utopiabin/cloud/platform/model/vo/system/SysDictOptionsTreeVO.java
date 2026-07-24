package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
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
public class SysDictOptionsTreeVO extends JsonSerializable {

    private Long id;
    private Long parentId;
    private String name;
    private String value;
    private List<SysDictOptionsTreeVO> children;

    public SysDictOptionsTreeVO(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
