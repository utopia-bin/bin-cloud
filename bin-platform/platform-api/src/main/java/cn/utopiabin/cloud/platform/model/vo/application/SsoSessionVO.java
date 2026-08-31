package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用会话管理视图，不返回任何令牌")
public class SsoSessionVO extends JsonSerializable {
    @Schema(description = "会话随机ID")
    private String sessionId;
    @Schema(description = "来源平台会话ID")
    private String parentSessionId;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "应用ID")
    private Long applicationId;
    @Schema(description = "实例ID")
    private Long tenantApplicationId;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "应用名称")
    private String applicationName;
    @Schema(description = "用户账号")
    private String username;
    @Schema(description = "ACTIVE、REVOKED或EXPIRED")
    private String status;
    @Schema(description = "认证时间")
    private LocalDateTime authTime;
    @Schema(description = "绝对到期时间")
    private LocalDateTime expireAt;
    @Schema(description = "撤销时间")
    private LocalDateTime revokedAt;
    @Schema(description = "稳定撤销原因")
    private String revokeReason;
}
