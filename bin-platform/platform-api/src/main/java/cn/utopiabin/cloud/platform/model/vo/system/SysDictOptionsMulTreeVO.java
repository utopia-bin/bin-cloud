package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
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
public class SysDictOptionsMulTreeVO extends JsonSerializable {

    private String code;
    private List<SysDictOptionsTreeVO> data;
}
