package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用成员授权详情")
public class UserApplicationVO extends JsonSerializable {
    @Schema(description = "授权ID")
    private Long id;
    @Schema(description = "授权版本")
    private Integer version;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "开通实例ID")
    private Long tenantApplicationId;
    @Schema(description = "用户账号")
    private String username;
    @Schema(description = "ACTIVE或DISABLED")
    private String status;
    @Schema(description = "生效时间")
    private LocalDateTime effectiveAt;
    @Schema(description = "到期时间")
    private LocalDateTime expireAt;
    @Schema(description = "说明")
    private String comment;
    @Schema(description = "本实例角色ID")
    private List<Long> roleIds;
}
