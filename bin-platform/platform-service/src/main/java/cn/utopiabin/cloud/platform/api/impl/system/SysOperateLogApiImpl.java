package cn.utopiabin.cloud.platform.api.impl.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.api.system.SysOperateLogApi;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.system.SysOperateLogPageQuery;
import cn.utopiabin.cloud.platform.model.vo.system.SysOperateLogVO;
import cn.utopiabin.cloud.platform.service.system.SysOperateLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 操作日志 API 实现
 * <p>
 * 委托 {@link SysOperateLogService} 处理查询逻辑。
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志 Dubbo 服务实现")
public class SysOperateLogApiImpl implements SysOperateLogApi {

    private final SysOperateLogService operateLogService;

    @Override
    @RequirePermission("platform:operate-log:read")
    public PageResult<SysOperateLogVO> page(SysOperateLogPageQuery query) {
        return operateLogService.page(query);
    }
}
