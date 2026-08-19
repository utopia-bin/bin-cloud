package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统菜单
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
@Schema(description = "系统菜单")
public class SysMenu extends BaseEntity {

    /**
     * 父级 ID (顶级为 0)
     */
    @Schema(description = "父级ID，顶级为0")
    private Long parentId;

    /**
     * 菜单类型: 1 目录 2 菜单 3 按钮
     */
    @Schema(description = "菜单类型: 1目录 2菜单 3按钮")
    private Integer type;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    private String name;

    /**
     * 路由路径
     */
    @Schema(description = "路由路径")
    private String path;

    /**
     * 组件路径
     */
    @Schema(description = "组件路径")
    private String component;

    /**
     * 菜单图标
     */
    @Schema(description = "菜单图标")
    private String icon;

    /**
     * 权限标识 (如 system:user:add)
     */
    @Schema(description = "权限标识")
    private String permission;

    /**
     * 排序码
     */
    @Schema(description = "排序码")
    private Integer sort;

    /**
     * 是否可见
     */
    @Schema(description = "是否可见")
    private Boolean visible;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean available;
}
