package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.system.SysOperateLog;
import cn.utopiabin.cloud.platform.mapper.system.SysOperateLogMapper;
import cn.utopiabin.cloud.platform.model.dto.system.SysOperateLogPageQuery;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

/**
 * 操作日志 Repository
 * <p>
 * 注意: sys_operate_log 在租户忽略清单中 (SQL 插件不追加 tenant_id 条件)，
 * 查询时由本类手动控制租户边界：存在租户上下文时只能查看本租户的审计日志。
 *
 * @since 1.0
 */
@Repository
public class SysOperateLogRepository extends BaseRepository<SysOperateLogMapper, SysOperateLog> {

    @Override
    protected String getNotFoundMessage() {
        return "操作日志不存在";
    }

    /**
     * 分页查询 (按模块/操作人/结果过滤，操作时间倒序)
     * <p>
     * 租户边界：租户调用方强制限定本租户数据；平台调用必须使用无租户上下文的受控入口。
     */
    public Page<SysOperateLog> page(SysOperateLogPageQuery query) {
        Long tenantId = currentTenantId();
        return page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysOperateLog>()
                        .eq(StrUtil.isNotBlank(query.getModule()), SysOperateLog::getModule, query.getModule())
                        .eq(StrUtil.isNotBlank(query.getOperateUsername()),
                                SysOperateLog::getOperateUsername, query.getOperateUsername())
                        .eq(query.getSuccess() != null, SysOperateLog::getSuccess, query.getSuccess())
                        .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                                .like(SysOperateLog::getModule, query.getKeyword())
                                .or().like(SysOperateLog::getAction, query.getKeyword())
                                .or().like(SysOperateLog::getOperateUsername, query.getKeyword()))
                        // 手动租户边界：存在租户上下文时只查本租户
                        .eq(tenantId != null, SysOperateLog::getTenantId, tenantId)
                        .orderByDesc(SysOperateLog::getOperateTime));
    }

    /**
     * 当前租户 ID。无上下文代表受控的平台调用；非法上下文使用兜底值，禁止放宽数据边界。
     */
    Long currentTenantId() {
        String tenantId = UserContextHolder.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            return null;
        }
        try {
            return Long.valueOf(tenantId.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
