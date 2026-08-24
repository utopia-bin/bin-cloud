package cn.utopiabin.cloud.platform.entity.system;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 操作日志
 * <p>
 * 由 {@code @OperateLog} 切面异步记录，用于安全审计与问题追溯。
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_operate_log")
@Schema(description = "操作日志")
public class SysOperateLog extends BaseEntity {

    /**
     * 业务模块 (如 "用户管理")
     */
    @Schema(description = "业务模块")
    private String module;

    /**
     * 操作动作 (如 "新增用户")
     */
    @Schema(description = "操作动作")
    private String action;

    /**
     * 操作类型: CREATE/UPDATE/DELETE/QUERY/ASSIGN/ENABLE/AUTH/OTHER
     */
    @Schema(description = "操作类型")
    private String type;

    /**
     * 调用方法 (类名.方法名)
     */
    @Schema(description = "调用方法")
    private String method;

    /**
     * 参数摘要 (JSON，敏感字段已脱敏，超长截断)
     */
    @Schema(description = "参数摘要")
    private String params;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;

    /**
     * 异常消息 (成功时为空)
     */
    @Schema(description = "异常消息")
    private String errorMsg;

    /**
     * 耗时 (毫秒)
     */
    @Schema(description = "耗时(毫秒)")
    private Long costMs;

    /**
     * 操作人 ID
     */
    @Schema(description = "操作人ID")
    private String operateUserId;

    /**
     * 操作人用户名
     */
    @Schema(description = "操作人用户名")
    private String operateUsername;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间")
    private Date operateTime;
}
