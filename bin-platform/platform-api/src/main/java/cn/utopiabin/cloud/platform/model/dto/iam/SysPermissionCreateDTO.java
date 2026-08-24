package cn.utopiabin.cloud.platform.model.dto.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限资源创建命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysPermissionCreateDTO extends JsonSerializable {

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称长度不能超过50个字符")
    private String name;

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100个字符")
    @Pattern(regexp = "^[a-z][a-z0-9]*(?::[a-z][a-z0-9]*){2,}$|^\\*$",
            message = "权限编码格式应为 domain:resource:action")
    private String code;

    @Size(max = 200, message = "权限描述长度不能超过200个字符")
    private String description;

    private Boolean available;

    private Integer sort;
}
