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
@Schema(description = "租户应用开通或生命周期调整")
public class InstanceDTO extends JsonSerializable {
    @Schema(description = "编辑时指定原实例；关闭后恢复复用原ID")
    private Long id;
    @Schema(description = "编辑时必填的乐观锁版本")
    private Integer expectedVersion;
    @Schema(description = "所属租户ID")
    @NotNull @Positive
    private Long tenantId;
    @Schema(description = "待开通的应用产品ID")
    @NotNull @Positive
    private Long applicationId;
    @Schema(description = "首次开通的应用管理员，必须是该租户有效用户")
    private Long adminUserId;
    @Schema(description = "ACTIVE开通、SUSPENDED暂停、CLOSED关闭；到期状态实时推导")
    @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED|CLOSED")
    @NotNull
    private String status = "ACTIVE";
    @Schema(description = "ALL全部有效用户；ASSIGNED仅显式准入用户")
    @NotBlank @Pattern(regexp = "ALL|ASSIGNED")
    @NotNull
    private String accessPolicy = "ASSIGNED";
    @Schema(description = "生效时间，空为立即；格式yyyy-MM-ddTHH:mm:ss")
    private LocalDateTime effectiveAt;
    @Schema(description = "过期时间，空为长期；必须晚于生效时间")
    private LocalDateTime expireAt;
    @Schema(description = "可选专属入口，不作为回调白名单")
    @Size(max = 500)
    @NotNull
    private String entryUrlOverride = "";
    @Schema(description = "开通说明")
    @Size(max = 500)
    @NotNull
    private String comment = "";
}
