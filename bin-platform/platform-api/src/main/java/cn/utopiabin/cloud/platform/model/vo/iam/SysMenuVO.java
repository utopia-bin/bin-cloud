package cn.utopiabin.cloud.platform.model.vo.iam;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统菜单 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统菜单")
public class SysMenuVO extends BaseVO {

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "菜单类型: 1目录 2菜单 3按钮")
    private Integer type;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "排序码")
    private Integer sort;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "是否启用")
    private Boolean available;
}
