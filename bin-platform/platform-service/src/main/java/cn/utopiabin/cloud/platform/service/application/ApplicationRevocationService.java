package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.platform.repository.application.ApplicationSessionRepository;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationRevocationService {
    private final ApplicationSessionRepository repository;
    private final SsoAuditService audit;

    @Transactional
    public void application(long app, String reason) {
        repository.revokeByApplication(reason, app);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, null, app, null, null, null));
    }

    @Transactional
    public void instance(long tenant, long instance, String reason) {
        repository.revokeByInstance(reason, tenant, instance);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, instance, null, null));
    }

    @Transactional
    public void user(long tenant, long user, String reason) {
        repository.revokeByUser(reason, tenant, user);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, null, user, null));
    }

    @Transactional
    public void tenant(long tenant, String reason) {
        repository.revokeByTenant(reason, tenant);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, null, null, null));
    }

    @Transactional
    public void member(long tenant, long instance, long user, String reason) {
        repository.revokeByMember(reason, tenant, instance, user);
        TransactionAfterCommitExecutor.afterCommit(
                () -> audit.record("REVOKED", true, reason, tenant, null, instance, user, null));
    }
}
