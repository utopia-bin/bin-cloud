package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限资源创建命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限资源新增参数")
public class SysPermissionCreateDTO extends JsonSerializable {

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称长度不能超过50个字符")
    @Schema(description = "权限显示名称，最长 50 个字符", example = "用户查询",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100个字符")
    @Pattern(regexp = "^[a-z][a-z0-9]*(?::[a-z][a-z0-9]*){2,}$|^\\*$",
            message = "权限编码格式应为 domain:resource:action")
    @Schema(description = "权限唯一编码，格式为 domain:resource:action；星号表示全部权限",
            example = "system:user:list", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Size(max = 200, message = "权限描述长度不能超过200个字符")
    @Schema(description = "权限用途说明，最长 200 个字符", example = "允许查看系统用户列表")
    private String description;

    @Schema(description = "权限是否可用；为空时由服务端采用默认值", example = "true")
    private Boolean available;

    @Schema(description = "展示顺序，数值越小越靠前", example = "10")
    private Integer sort;
}
