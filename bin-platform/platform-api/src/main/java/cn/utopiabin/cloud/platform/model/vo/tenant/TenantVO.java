package cn.utopiabin.cloud.platform.model.vo.tenant;

import cn.utopiabin.cloud.common.model.vo.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户信息")
public class TenantVO extends BaseVO {

    @Schema(description = "租户名称")
    private String name;

    @Schema(description = "租户编码")
    private String code;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "到期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否启用")
    private Boolean available;

    @Schema(description = "排序码")
    private Integer sort;

    @Schema(description = "备注")
    private String comment;
}
