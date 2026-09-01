package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SSO审计记录，不记录授权码、Token或URL查询参数")
public class SsoAuditVO extends JsonSerializable {
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "可确定时的租户ID")
    private Long tenantId;

    @Schema(description = "可确定时的应用ID")
    private Long applicationId;

    @Schema(description = "可确定时的实例ID")
    private Long tenantApplicationId;

    @Schema(description = "可确定时的用户ID")
    private Long userId;

    @Schema(description = "授权、兑换、刷新、退出或撤销事件")
    private String eventType;

    @Schema(description = "结果")
    private boolean success;

    @Schema(description = "稳定错误码")
    private String failureCode;

    @Schema(description = "关联会话ID")
    private String sessionId;

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "发生时间")
    private LocalDateTime eventTime;
}
