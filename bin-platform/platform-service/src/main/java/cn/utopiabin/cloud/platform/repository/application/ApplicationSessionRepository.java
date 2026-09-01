package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuditVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoSessionVO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.Map;

/** SSO 会话与审计数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationSessionRepository extends ApplicationRepositorySupport {
    private final ApplicationPersistenceMapper mapper;

    public PageResult<SsoSessionVO> pageSessions(ApplicationQuery query, Long tenantId) {
        Map<String, Object> parameters = pageParameters(query, tenantId);
        Long total = scalarLong(mapper.sessionCount(parameters));
        return page(query, total, mapper.sessionPage(parameters), SsoSessionVO.class);
    }

    public PageResult<SsoAuditVO> pageAuditLogs(ApplicationQuery query, Long tenantId) {
        Map<String, Object> parameters = pageParameters(query, tenantId);
        Long total = scalarLong(mapper.auditCount(parameters));
        return page(query, total, mapper.auditPage(parameters), SsoAuditVO.class);
    }

    public Map<String, Object> lockSession(String sessionId) {
        return one(mapper.selectSsoSessionForUpdate(parameters(sessionId)));
    }

    public int revokeSession(long tenantId, String sessionId) {
        return mapper.revokeSsoSessionByAdmin(parameters(tenantId, sessionId, sessionId));
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
        return mapper.insertSsoAudit(
                parameters(
                        id,
                        tenantId,
                        applicationId,
                        instanceId,
                        userId,
                        event,
                        success,
                        failure,
                        sessionId,
                        traceId));
    }

    public int revokeByApplication(String reason, long applicationId) {
        return mapper.revokeByApplication(parameters(reason, applicationId));
    }

    public int revokeByInstance(String reason, long tenantId, long instanceId) {
        return mapper.revokeByInstance(parameters(reason, tenantId, instanceId));
    }

    public int revokeByUser(String reason, long tenantId, long userId) {
        return mapper.revokeByUser(parameters(reason, tenantId, userId));
    }

    public int revokeByTenant(String reason, long tenantId) {
        return mapper.revokeByTenant(parameters(reason, tenantId));
    }

    public int revokeByMember(String reason, long tenantId, long instanceId, long userId) {
        return mapper.revokeByMember(parameters(reason, tenantId, instanceId, userId));
    }
}
