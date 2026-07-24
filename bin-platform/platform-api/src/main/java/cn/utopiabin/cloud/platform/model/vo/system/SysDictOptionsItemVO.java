package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项 VO（精简字段，用于缓存和级联展示）
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictOptionsItemVO extends JsonSerializable {

    private Long id;
    private Long parentId;
    private String code;
    private String optionName;
    private String optionValue;
}
