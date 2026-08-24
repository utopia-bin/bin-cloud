package cn.utopiabin.cloud.platform.model.vo.system;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 操作日志 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "操作日志")
public class SysOperateLogVO extends JsonSerializable {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "操作动作")
    private String action;

    @Schema(description = "操作类型")
    private String type;

    @Schema(description = "调用方法")
    private String method;

    @Schema(description = "参数摘要（已脱敏）")
    private String params;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "异常消息")
    private String errorMsg;

    @Schema(description = "耗时(毫秒)")
    private Long costMs;

    @Schema(description = "操作人ID")
    private String operateUserId;

    @Schema(description = "操作人用户名")
    private String operateUsername;

    @Schema(description = "操作时间")
    private Date operateTime;
}
