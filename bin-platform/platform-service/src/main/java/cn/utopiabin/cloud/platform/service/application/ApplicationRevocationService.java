package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationRevocationService {
    private final ApplicationStore store;
    private final SsoAuditService audit;

    @Transactional
    public void application(long app, String reason) {
        store.update("revokeByApplication", reason, app);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, null, app, null, null, null));
    }

    @Transactional
    public void instance(long tenant, long instance, String reason) {
        store.update("revokeByInstance", reason, tenant, instance);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, instance, null, null));
    }

    @Transactional
    public void user(long tenant, long user, String reason) {
        store.update("revokeByUser", reason, tenant, user);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, null, user, null));
    }

    @Transactional
    public void tenant(long tenant, String reason) {
        store.update("revokeByTenant", reason, tenant);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, null, null, null));
    }

    @Transactional
    public void member(long tenant, long instance, long user, String reason) {
        store.update("revokeByMember", reason, tenant, instance, user);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, instance, user, null));
    }
}
