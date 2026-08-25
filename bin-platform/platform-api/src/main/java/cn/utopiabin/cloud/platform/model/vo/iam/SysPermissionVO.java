package cn.utopiabin.cloud.platform.model.vo.iam;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限资源。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限资源信息")
public class SysPermissionVO extends BaseVO {

    @Schema(description = "权限显示名称", example = "用户查询")
    private String name;

    @Schema(description = "权限唯一编码，格式为 domain:resource:action", example = "system:user:list")
    private String code;

    @Schema(description = "权限用途说明", example = "允许查看系统用户列表")
    private String description;

    @Schema(description = "权限是否可用", example = "true")
    private Boolean available;

    @Schema(description = "展示顺序，数值越小越靠前", example = "10")
    private Integer sort;

    @Schema(description = "乐观锁版本号", example = "1")
    private Integer version;
}
