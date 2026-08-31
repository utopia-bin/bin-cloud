package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "仅返回给应用后端的Token结果")
public class SsoTokenVO extends JsonSerializable {
    @Schema(description = "短期应用JWT，禁止放URL或日志")
    private String accessToken;
    @Schema(description = "轮换刷新凭证，禁止下发浏览器或记录日志")
    private String refreshToken;
    @Schema(description = "Access Token剩余秒数")
    private long expiresIn;
    @Schema(description = "应用会话ID")
    private String sessionId;
    @Schema(description = "Token受众")
    private String applicationCode;
}
