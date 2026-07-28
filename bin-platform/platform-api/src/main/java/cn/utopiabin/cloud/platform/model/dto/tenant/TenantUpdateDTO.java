package cn.utopiabin.cloud.platform.model.dto.tenant;

import cn.utopiabin.cloud.common.model.dto.IdDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租户编辑 DTO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户编辑参数")
public class TenantUpdateDTO extends IdDTO {

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称长度不能超过100个字符")
    @Schema(description = "租户名称", example = "测试租户", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 50, message = "租户编码长度不能超过50个字符")
    @Schema(description = "租户编码（唯一）", example = "test_tenant", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Size(max = 50, message = "联系人长度不能超过50个字符")
    @Schema(description = "联系人", example = "张三")
    private String contactName;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    @Schema(description = "联系邮箱", example = "zhangsan@example.com")
    private String contactEmail;

    @Schema(description = "到期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "排序码", example = "10")
    private Integer sort;

    @Schema(description = "备注")
    private String comment;
}
