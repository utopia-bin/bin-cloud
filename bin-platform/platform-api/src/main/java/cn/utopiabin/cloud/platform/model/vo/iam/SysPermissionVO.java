package cn.utopiabin.cloud.platform.model.vo.iam;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限资源。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysPermissionVO extends BaseVO {
    private String name;
    private String code;
    private String description;
    private Boolean available;
    private Integer sort;
    private Integer version;
}
