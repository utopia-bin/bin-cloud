package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "短时授权回调结果，不包含平台Token")
public class SsoAuthorizeVO extends JsonSerializable {
    @Schema(description = "仅包含一次性code和state的精确回调地址")
    private String redirectUrl;
    @Schema(description = "授权码有效秒数")
    private int expiresIn;
}
