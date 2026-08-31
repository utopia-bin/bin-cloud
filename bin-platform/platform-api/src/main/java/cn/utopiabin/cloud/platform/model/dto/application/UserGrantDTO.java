package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用成员准入与角色全量替换参数")
public class UserGrantDTO extends JsonSerializable {
    @Schema(description = "目标实例ID")
    @NotNull @Positive
    private Long tenantApplicationId;
    @Schema(description = "同租户用户ID")
    @NotNull @Positive
    private Long userId;
    @Schema(description = "已有授权编辑时必填；新增留空")
    private Integer expectedVersion;
    @Schema(description = "ACTIVE允许进入、DISABLED撤销准入")
    @NotBlank @Pattern(regexp = "ACTIVE|DISABLED")
    @NotNull
    private String status = "ACTIVE";
    @Schema(description = "用户授权生效时间，空为立即")
    private LocalDateTime effectiveAt;
    @Schema(description = "用户授权到期时间，空为长期")
    private LocalDateTime expireAt;
    @Schema(description = "该实例内角色ID，空列表清空角色")
    @NotNull @Size(max = 100)
    private List<@NotNull @Positive Long> roleIds = new java.util.ArrayList<>();
    @Schema(description = "授权说明")
    @Size(max = 500)
    @NotNull
    private String comment = "";
}
