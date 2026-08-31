package cn.utopiabin.cloud.platform.model.dto.tenant;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户管理员初始化凭证；只用于首次开通，不覆盖已有管理员")
public class TenantAdminDTO extends JsonSerializable {
    @NotBlank(message = "管理员账号不能为空")
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.-]{0,49}", message = "管理员账号须为1至50位字母、数字、下划线、点或连字符")
    @Schema(description = "租户内唯一的初始管理员登录账号", example = "tenant_admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminUsername;

    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 8, max = 72, message = "管理员密码长度须为8至72位")
    @Schema(description = "初始管理员密码，须满足平台密码强度策略；仅接收，不返回或记录", accessMode = Schema.AccessMode.WRITE_ONLY,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminPassword;
}
