package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.number;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuditVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoSessionVO;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationSessionAdminService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final SsoAuditService audit;

    public PageResult<SsoSessionVO> sessions(ApplicationQuery q) {
        boundary.require("audit");
        Long tenantId =
                q.getTenantId() != null || !boundary.global()
                        ? boundary.queryTenant(q.getTenantId())
                        : null;
        return store.page(SsoSessionVO.class, q, "sessionCount", "sessionPage", tenantId);
    }

    public PageResult<SsoAuditVO> logs(ApplicationQuery q) {
        boundary.require("audit");
        Long tenantId =
                q.getTenantId() != null || !boundary.global()
                        ? boundary.queryTenant(q.getTenantId())
                        : null;
        return store.page(SsoAuditVO.class, q, "auditCount", "auditPage", tenantId);
    }

    @Transactional
    @OperateLog(module = "应用会话", action = "强制下线", type = OperateType.AUTH, maskParams = true)
    public void revoke(String sid) {
        boundary.require("revoke");
        var session = store.one("applicationSessionAdminServiceSelect01", sid);
        long tenant = number(session, "tenant_id");
        boundary.queryTenant(tenant);
        store.update("applicationSessionAdminServiceUpdate02", tenant, sid, sid);
        TransactionAfterCommitExecutor.afterCommit(
                () ->
                        audit.record(
                                "REVOKED",
                                true,
                                "ADMIN_REVOKED",
                                tenant,
                                number(session, "application_id"),
                                number(session, "tenant_application_id"),
                                number(session, "user_id"),
                                sid));
    }
}
