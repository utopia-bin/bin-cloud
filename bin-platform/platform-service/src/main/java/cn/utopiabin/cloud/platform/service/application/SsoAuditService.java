package cn.utopiabin.cloud.platform.service.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SsoAuditService {
    private final ApplicationStore store;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String event,
            boolean success,
            String failure,
            Long tenant,
            Long app,
            Long instance,
            Long user,
            String sid) {
        String trace = MDC.get("traceId");
        store.update(
                "insertSsoAudit",
                IdWorker.getId(),
                tenant,
                app,
                instance,
                user,
                event,
                success,
                failure == null ? "" : failure,
                sid == null ? "" : sid,
                trace == null ? "" : trace.substring(0, Math.min(trace.length(), 64)));
    }
}
