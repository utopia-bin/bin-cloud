package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用权限或菜单资源保存参数")
public class ApplicationResourceDTO extends JsonSerializable {
    @Schema(description = "资源ID，新增留空")
    private Long id;

    @Schema(description = "编辑必填的乐观锁版本")
    private Integer expectedVersion;

    @Schema(description = "资源所属应用产品")
    @NotNull
    @Positive
    private Long applicationId;

    @Schema(description = "资源名称")
    @NotBlank
    @Size(max = 50)
    private String name;

    @Schema(description = "权限编码，仅权限资源使用")
    @Size(max = 100)
    @NotNull
    private String code = "";

    @Schema(description = "权限用途说明")
    @Size(max = 200)
    @NotNull
    private String description = "";

    @Schema(description = "同应用父菜单ID，0为根")
    private Long parentId = 0L;

    @Schema(description = "菜单类型：1目录、2页面、3按钮")
    @Min(1)
    @Max(3)
    private int type = 2;

    @Schema(description = "应用内路由路径")
    @Size(max = 200)
    @NotNull
    private String path = "";

    @Schema(description = "应用前端已实现的组件键")
    @Size(max = 200)
    @NotNull
    private String component = "";

    @Schema(description = "ElementPlus图标名称")
    @Size(max = 100)
    @NotNull
    private String icon = "";

    @Schema(description = "控制菜单显示的本应用权限编码")
    @Size(max = 100)
    @NotNull
    private String permission = "";

    @Schema(description = "稳定的应用内路由名")
    @Size(max = 100)
    @NotNull
    private String routeName = "";

    @Schema(description = "一期支持INTERNAL内部导航")
    @Pattern(regexp = "INTERNAL")
    @NotNull
    private String openMode = "INTERNAL";

    @Schema(description = "菜单是否显示")
    private boolean visible = true;

    @Schema(description = "资源是否启用")
    private boolean available = true;

    @Schema(description = "资源显示顺序")
    @Min(0)
    private int sort = 10;
}
