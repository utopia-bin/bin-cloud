package cn.utopiabin.cloud.platform.api.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.system.SysOperateLogPageQuery;
import cn.utopiabin.cloud.platform.model.vo.system.SysOperateLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 操作日志 Dubbo API
 * <p>
 * 提供操作日志的审计查询能力 (由 @OperateLog 切面自动记录)。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "操作日志", description = "操作日志审计查询")
public interface SysOperateLogApi {

    @Operation(summary = "分页查询操作日志", description = "支持按模块/操作人/结果/关键字过滤，操作时间倒序")
    PageResult<SysOperateLogVO> page(@Parameter(description = "分页查询条件", required = true) SysOperateLogPageQuery query);
}
