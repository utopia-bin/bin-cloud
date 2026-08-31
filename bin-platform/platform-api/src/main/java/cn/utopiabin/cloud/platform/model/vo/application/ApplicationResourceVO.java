package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用权限或菜单资源")
public class ApplicationResourceVO extends JsonSerializable {
    @Schema(description = "资源ID")
    private Long id;
    @Schema(description = "资源版本")
    private Integer version;
    @Schema(description = "所属产品ID")
    private Long applicationId;
    @Schema(description = "资源名称")
    private String name;
    @Schema(description = "权限编码")
    private String code;
    @Schema(description = "权限说明")
    private String description;
    @Schema(description = "父菜单ID")
    private Long parentId;
    @Schema(description = "菜单类型")
    private Integer type;
    @Schema(description = "路由")
    private String path;
    @Schema(description = "组件键")
    private String component;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "菜单所需权限")
    private String permission;
    @Schema(description = "路由名称")
    private String routeName;
    @Schema(description = "打开方式")
    private String openMode;
    @Schema(description = "可见")
    private boolean visible;
    @Schema(description = "启用")
    private boolean available;
    @Schema(description = "排序")
    private int sort;
}
