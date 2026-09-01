package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户应用实例与当前有效状态")
public class TenantApplicationVO extends JsonSerializable {
    @Schema(description = "开通实例ID")
    private Long id;

    @Schema(description = "当前版本")
    private Integer version;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "应用产品ID")
    private Long applicationId;

    @Schema(description = "应用受众编码")
    private String applicationCode;

    @Schema(description = "应用名称")
    private String applicationName;

    @Schema(description = "持久化生命周期状态")
    private String status;

    @Schema(description = "结合时间窗口和产品、租户状态计算的当前状态")
    private String effectiveStatus;

    @Schema(description = "ALL或ASSIGNED")
    private String accessPolicy;

    @Schema(description = "专属导航入口")
    private String entryUrlOverride;

    @Schema(description = "最终入口，仅在准入通过后可用")
    private String entryUrl;

    @Schema(description = "应用图标")
    private String iconUrl;

    @Schema(description = "首次开通时间")
    private LocalDateTime openedAt;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveAt;

    @Schema(description = "到期时间")
    private LocalDateTime expireAt;

    @Schema(description = "说明")
    private String comment;
}
