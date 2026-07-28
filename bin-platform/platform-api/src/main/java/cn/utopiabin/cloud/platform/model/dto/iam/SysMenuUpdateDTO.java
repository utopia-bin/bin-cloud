package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.model.dto.IdDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统菜单编辑 DTO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统菜单编辑参数")
public class SysMenuUpdateDTO extends IdDTO {

    @Schema(description = "父级ID，顶级为0", example = "0")
    private Long parentId;

    @Schema(description = "菜单类型: 1目录 2菜单 3按钮", example = "2")
    private Integer type;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    @Schema(description = "菜单名称", example = "用户管理", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 200, message = "路由路径长度不能超过200个字符")
    @Schema(description = "路由路径", example = "/system/user")
    private String path;

    @Size(max = 200, message = "组件路径长度不能超过200个字符")
    @Schema(description = "组件路径", example = "system/user/index")
    private String component;

    @Size(max = 100, message = "菜单图标长度不能超过100个字符")
    @Schema(description = "菜单图标", example = "user")
    private String icon;

    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    @Schema(description = "权限标识", example = "system:user:list")
    private String permission;

    @Schema(description = "排序码", example = "10")
    private Integer sort;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "是否启用")
    private Boolean available;
}
