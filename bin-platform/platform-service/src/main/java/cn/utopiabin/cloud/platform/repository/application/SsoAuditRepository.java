package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.entity.application.SysSsoLoginLog;
import cn.utopiabin.cloud.platform.mapper.application.SsoAuditMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuditVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/** SSO审计事件数据仓库。 */
@Repository
@RequiredArgsConstructor
public class SsoAuditRepository extends ApplicationRepositorySupport {

  private final SsoAuditMapper mapper;

  public PageResult<SsoAuditVO> pageAuditLogs(ApplicationQuery query, Long tenantId) {
    int page = Math.max(1, query.getPage());
    int size = Math.max(1, Math.min(100, query.getSize()));
    LambdaQueryWrapper<SysSsoLoginLog> conditions =
        new LambdaQueryWrapper<SysSsoLoginLog>()
            .eq(tenantId != null, SysSsoLoginLog::getTenantId, tenantId)
            .eq(
                query.getApplicationId() != null,
                SysSsoLoginLog::getApplicationId,
                query.getApplicationId())
            .eq(
                query.getTenantApplicationId() != null,
                SysSsoLoginLog::getTenantApplicationId,
                query.getTenantApplicationId())
            .eq("SUCCESS".equals(query.getStatus()), SysSsoLoginLog::getSuccess, true)
            .eq("FAILURE".equals(query.getStatus()), SysSsoLoginLog::getSuccess, false)
            .orderByDesc(SysSsoLoginLog::getEventTime, SysSsoLoginLog::getId);
    Page<SysSsoLoginLog> result = mapper.selectPage(new Page<>(page, size), conditions);
    return PageResult.of(
        page,
        size,
        result.getTotal(),
        result.getRecords().stream()
            .map(
                event -> {
                  SsoAuditVO view = new SsoAuditVO();
                  BeanUtils.copyProperties(event, view);
                  return view;
                })
            .toList());
  }

  public int insertAuditLog(
      long id,
      Long tenantId,
      Long applicationId,
      Long instanceId,
      Long userId,
      String event,
      boolean success,
      String failure,
      String sessionId,
      String traceId) {
    SysSsoLoginLog log = new SysSsoLoginLog();
    log.setId(id);
    log.setTenantId(tenantId);
    log.setApplicationId(applicationId);
    log.setTenantApplicationId(instanceId);
    log.setUserId(userId);
    log.setEventType(event);
    log.setSuccess(success);
    log.setFailureCode(failure);
    log.setSessionId(sessionId);
    log.setTraceId(traceId);
    return mapper.insert(log);
  }
}
