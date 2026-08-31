package cn.utopiabin.cloud.platform.model.vo.iam;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统角色")
public class SysRoleVO extends BaseVO {

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "数据权限范围: 1租户内全部 4仅本人（业务数据范围预留，管理接口按功能权限和租户隔离）")
    private Integer dataScope;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "排序码")
    private Integer sort;

    @Schema(description = "备注")
    private String comment;
}
