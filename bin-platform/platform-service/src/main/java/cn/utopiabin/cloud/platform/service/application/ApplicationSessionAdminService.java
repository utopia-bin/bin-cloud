package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuditVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoSessionVO;
import cn.utopiabin.cloud.platform.repository.application.ApplicationSessionRepository;
import cn.utopiabin.cloud.platform.repository.application.SsoAuditRepository;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationSessionAdminService {
  private final ApplicationSessionRepository repository;
  private final SsoAuditRepository auditRepository;
  private final ApplicationBoundary boundary;
  private final SsoAuditService audit;

  @RequirePermission("platform:application:audit")
  public PageResult<SsoSessionVO> sessions(ApplicationQuery q) {
    return repository.pageSessions(q, boundary.queryTenant(q.getTenantId()));
  }

  @RequirePermission("platform:application:audit")
  public PageResult<SsoAuditVO> logs(ApplicationQuery q) {
    return auditRepository.pageAuditLogs(q, boundary.queryTenant(q.getTenantId()));
  }

  @Transactional
  @RequirePermission("platform:application:revoke")
  @OperateLog(module = "应用会话", action = "强制下线", type = OperateType.AUTH, maskParams = true)
  public void revoke(String sid) {
    var session = repository.lockSession(sid);
    long tenant = session.getTenantId();
    boundary.queryTenant(tenant);
    repository.revokeSession(tenant, sid);
    TransactionAfterCommitExecutor.afterCommit(
        () ->
            audit.record(
                "REVOKED",
                true,
                "ADMIN_REVOKED",
                tenant,
                session.getApplicationId(),
                session.getTenantApplicationId(),
                session.getUserId(),
                sid));
  }
}
