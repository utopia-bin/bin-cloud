package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictVO extends BaseVO {

    private String name;
    private String code;
    private String comment;
    private Integer sort;
    private Boolean available;
}
