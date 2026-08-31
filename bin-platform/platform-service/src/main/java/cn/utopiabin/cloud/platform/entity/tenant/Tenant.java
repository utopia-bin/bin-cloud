package cn.utopiabin.cloud.platform.entity.tenant;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租户
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_tenant", excludeProperty = "tenantId")
@Schema(description = "租户")
public class Tenant extends BaseEntity {

    /**
     * 租户名称
     */
    @Schema(description = "租户名称")
    private String name;

    /**
     * 租户编码 (唯一)
     */
    @Schema(description = "租户编码（唯一）")
    private String code;

    /**
     * 联系人
     */
    @Schema(description = "联系人")
    private String contactName;

    /**
     * 联系电话
     */
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @Schema(description = "联系邮箱")
    private String contactEmail;

    /**
     * 到期时间
     */
    @Schema(description = "到期时间")
    private LocalDateTime expireTime;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean available;

    /**
     * 排序码
     */
    @Schema(description = "排序码")
    private Integer sort;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String comment;
}
