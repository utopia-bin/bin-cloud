package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前应用会话身份与实时权限")
public class ApplicationProfileVO extends JsonSerializable {
    @Schema(description = "统一用户ID")
    private Long userId;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "用户账号")
    private String username;
    @Schema(description = "应用ID")
    private Long applicationId;
    @Schema(description = "开通实例ID")
    private Long tenantApplicationId;
    @Schema(description = "应用编码")
    private String applicationCode;
    @Schema(description = "应用名称")
    private String applicationName;
    @Schema(description = "会话ID")
    private String sessionId;
    @Schema(description = "当前有效应用角色")
    private List<String> roles;
    @Schema(description = "当前应用有效权限")
    private List<String> permissionCodes;
    @Schema(description = "当前应用菜单树")
    private List<cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO> menus;
}
