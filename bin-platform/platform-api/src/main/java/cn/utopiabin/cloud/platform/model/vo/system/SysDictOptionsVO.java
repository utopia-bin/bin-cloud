package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictOptionsVO extends BaseVO {

    private Long parentId;
    private Long dictId;
    private String optionName;
    private String optionValue;
    private String optionComment;
    private Integer sort;
}
