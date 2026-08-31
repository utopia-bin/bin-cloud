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
@Schema(description = "应用列表、开通、会话和审计的分页筛选")
public class ApplicationQuery extends JsonSerializable {
    @Schema(description = "页码，从1开始")
    @Min(1)
    private int page = 1;
    @Schema(description = "每页记录数，1至100")
    @Min(1) @Max(100)
    private int size = 10;
    @Schema(description = "指定租户，仅平台运营方可跨租户查看")
    private Long tenantId;
    @Schema(description = "应用产品ID")
    private Long applicationId;
    @Schema(description = "租户应用实例ID")
    private Long tenantApplicationId;
    @Schema(description = "按名称或编码筛选")
    @Size(max = 100)
    private String keyword;
    @Schema(description = "按对应资源的状态筛选")
    @Size(max = 16)
    private String status;
}
