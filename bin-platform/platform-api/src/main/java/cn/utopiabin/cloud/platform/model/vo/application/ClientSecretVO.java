package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "客户端凭证只展示一次，不可从查询接口取回")
public class ClientSecretVO extends JsonSerializable {
    @Schema(description = "后端客户端标识")
    private String clientId;
    @Schema(description = "仅本次返回的随机凭证，请配置到对应后端环境变量，禁止放到前端")
    private String clientSecret;
    @Schema(description = "更新后的应用版本")
    private Integer version;
}
