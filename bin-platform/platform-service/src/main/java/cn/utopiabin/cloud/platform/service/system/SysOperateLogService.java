package cn.utopiabin.cloud.platform.service.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.entity.system.SysOperateLog;
import cn.utopiabin.cloud.platform.model.dto.system.SysOperateLogPageQuery;
import cn.utopiabin.cloud.platform.model.vo.system.SysOperateLogVO;
import cn.utopiabin.cloud.platform.repository.system.SysOperateLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 操作日志服务
 * <p>
 * 提供操作日志的异步落库与分页查询能力。
 * 异步落库基于专用线程池 ({@code platformAsyncExecutor})，失败仅记日志不影响业务。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperateLogService {

    private final SysOperateLogRepository operateLogRepository;

    /**
     * 异步记录操作日志
     * <p>
     * 由 {@code OperateLogAspect} 调用，参数为上下文快照 (不依赖 ThreadLocal)。
     *
     * @param module     业务模块
     * @param action     操作动作
     * @param type       操作类型
     * @param method     调用方法
     * @param params     参数摘要 (已脱敏)
     * @param success    是否成功
     * @param errorMsg   异常消息
     * @param costMs     耗时 (毫秒)
     * @param userId     操作人 ID
     * @param username   操作人用户名
     * @param tenantId   租户 ID
     */
    @Async("platformAsyncExecutor")
    public void asyncRecord(String module, String action, String type, String method, String params,
                            boolean success, String errorMsg, long costMs,
                            String userId, String username, String tenantId, String traceId) {
        try {
            var entity = new SysOperateLog();
            entity.setApplicationId(1L);
            entity.setTraceId(traceId == null ? "" : traceId.substring(0, Math.min(64, traceId.length())));
            entity.setModule(module);
            entity.setAction(action);
            entity.setType(type);
            entity.setMethod(method);
            entity.setParams(params);
            entity.setSuccess(success);
            entity.setErrorMsg(errorMsg);
            entity.setCostMs(costMs);
            entity.setOperateUserId(userId);
            entity.setOperateUsername(username);
            entity.setOperateTime(new Date());
            if (tenantId != null && !tenantId.isBlank()) {
                try {
                    entity.setTenantId(Long.valueOf(tenantId.trim()));
                    entity.setTenantApplicationId(Long.valueOf(tenantId.trim()));
                } catch (NumberFormatException ignored) {
                    // 租户 ID 非数字时留空
                }
            }
            operateLogRepository.save(entity);
            log.debug("操作日志已记录: module={}, action={}, operator={}, cost={}ms",
                    module, action, username, costMs);
        } catch (Exception e) {
            // 日志失败不影响业务主流程
            log.warn("操作日志落库失败: module={}, action={}, error={}", module, action, e.getMessage());
        }
    }

    /**
     * 分页查询操作日志
     */
    public PageResult<SysOperateLogVO> page(SysOperateLogPageQuery query) {
        var page = operateLogRepository.page(query);
        var records = page.getRecords().stream()
                .map(l -> l.copyTo(SysOperateLogVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }
}
