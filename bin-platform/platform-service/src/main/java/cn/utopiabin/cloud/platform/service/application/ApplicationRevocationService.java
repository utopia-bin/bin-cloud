package cn.utopiabin.cloud.platform.service.application;

import lombok.RequiredArgsConstructor;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationRevocationService {
    private final JdbcTemplate jdbc;
    private final SsoAuditService audit;
    @Transactional
    public void application(long app, String reason) {
        jdbc.update("UPDATE sys_sso_session SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,revoke_reason=?,version=version+1 WHERE application_id=? AND status='ACTIVE'",reason,app);
        TransactionAfterCommitExecutor.afterCommit(() -> audit.record("REVOKED",true,reason,null,app,null,null,null));
    }
    @Transactional
    public void instance(long tenant,long instance,String reason) {
        jdbc.update("UPDATE sys_sso_session SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,revoke_reason=?,version=version+1 WHERE tenant_id=? AND tenant_application_id=? AND status='ACTIVE'",reason,tenant,instance);
        TransactionAfterCommitExecutor.afterCommit(() -> audit.record("REVOKED",true,reason,tenant,null,instance,null,null));
    }
    @Transactional
    public void user(long tenant,long user,String reason) {
        jdbc.update("UPDATE sys_sso_session SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,revoke_reason=?,version=version+1 WHERE tenant_id=? AND user_id=? AND status='ACTIVE'",reason,tenant,user);
        TransactionAfterCommitExecutor.afterCommit(() -> audit.record("REVOKED",true,reason,tenant,null,null,user,null));
    }
    @Transactional
    public void tenant(long tenant,String reason) {
        jdbc.update("UPDATE sys_sso_session SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,revoke_reason=?,version=version+1 WHERE tenant_id=? AND status='ACTIVE'",reason,tenant);
        TransactionAfterCommitExecutor.afterCommit(() -> audit.record("REVOKED",true,reason,tenant,null,null,null,null));
    }
    @Transactional
    public void member(long tenant,long instance,long user,String reason) {
        jdbc.update("UPDATE sys_sso_session SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,revoke_reason=?,version=version+1 WHERE tenant_id=? AND tenant_application_id=? AND user_id=? AND status='ACTIVE'",reason,tenant,instance,user);
        TransactionAfterCommitExecutor.afterCommit(() -> audit.record("REVOKED",true,reason,tenant,null,instance,user,null));
    }
}
