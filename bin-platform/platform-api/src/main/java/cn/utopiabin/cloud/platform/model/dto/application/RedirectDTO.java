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
@Schema(description = "应用精确回调白名单")
public class RedirectDTO extends JsonSerializable {
    @Schema(description = "DEV、TEST、STAGING、PROD；本地学习使用DEV")
    @NotBlank @Pattern(regexp = "DEV|TEST|STAGING|PROD")
    @NotNull
    private String environment = "DEV";
    @Schema(description = "完整回调地址，无查询串、片段或用户信息；DEV允许HTTP")
    @NotBlank @Size(max = 500)
    private String redirectUri;
    @Schema(description = "可选的退出落地页地址")
    @Size(max = 500)
    @NotNull
    private String logoutUri = "";
    @Schema(description = "是否允许此回调地址")
    private boolean available = true;
}
