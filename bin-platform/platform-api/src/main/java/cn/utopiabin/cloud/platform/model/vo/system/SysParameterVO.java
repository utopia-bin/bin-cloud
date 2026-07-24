package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysParameterVO extends BaseVO {

    private String paramKey;
    private String paramValue;
    private String paramComment;
    private Integer sort;
}
