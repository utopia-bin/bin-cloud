package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.changed;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.flag;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.time;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.version;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.window;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.within;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.InstanceDTO;
import cn.utopiabin.cloud.platform.model.vo.application.TenantApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantApplicationService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    public PageResult<TenantApplicationVO> page(ApplicationQuery q) {
        boundary.require("read");
        Long tenantId =
                q.getTenantId() != null || !boundary.global()
                        ? boundary.queryTenant(q.getTenantId())
                        : null;
        return store.page(
                TenantApplicationVO.class,
                q,
                "tenantApplicationCount",
                "tenantApplicationPage",
                tenantId);
    }

    public List<TenantApplicationVO> mine() {
        long tenant = boundary.tenantId(), user = boundary.userId();
        var list = store.list(TenantApplicationVO.class, "tenantApplicationMine", tenant);
        return list.stream()
                .filter(
                        item -> {
                            try {
                                boundary.access(tenant, user, item.getId());
                                return true;
                            } catch (BizException e) {
                                return false;
                            }
                        })
                .toList();
    }

    public void ensureConsole(long tenantId) {
        Long count = store.queryForObject("consoleInstanceCount", Long.class, tenantId);
        if (count == null || count == 0) {
            store.update("insertConsoleInstance", tenantId);
        }
    }

    @Transactional
    @OperateLog(module = "应用开通", action = "开通或调整租户应用", type = OperateType.UPDATE, maskParams = true)
    public long save(InstanceDTO dto) {
        boundary.require("provision");
        if (dto.getApplicationId() == 1) throw new BizException(400, "平台壳随租户自动开通，生命周期由租户管理控制");
        window(dto.getEffectiveAt(), dto.getExpireAt());
        SsoCrypto.navigation(dto.getEntryUrlOverride(), true);
        // 先锁应用产品再锁租户，与应用状态变更保持同一加锁顺序，避免并发死锁。
        var app = store.one("tenantApplicationServiceSelect01", dto.getApplicationId());
        var tenant = store.one("tenantApplicationServiceSelect02", dto.getTenantId());
        if ("ACTIVE".equals(dto.getStatus())
                && (!"ENABLED".equals(app.get("status"))
                        || !flag(tenant, "available")
                        || !within(null, time(tenant, "expire_time"), LocalDateTime.now())))
            throw new BizException(400, "租户或应用不可用，不能开通或恢复");
        if (dto.getId() != null) {
            var old =
                    store.one(
                            "tenantApplicationServiceSelect03",
                            dto.getId(),
                            dto.getTenantId(),
                            dto.getApplicationId());
            if (!List.of("ACTIVE", "SUSPENDED", "CLOSED", "EXPIRED", "PENDING")
                    .contains(old.get("status"))) throw new BizException(400, "不支持的原始状态");
            changed(
                    store.update(
                            "tenantApplicationServiceUpdate09",
                            dto.getStatus(),
                            dto.getAccessPolicy(),
                            dto.getEntryUrlOverride(),
                            dto.getEffectiveAt(),
                            dto.getExpireAt(),
                            dto.getComment(),
                            dto.getStatus(),
                            dto.getStatus(),
                            String.valueOf(boundary.userId()),
                            dto.getId(),
                            dto.getTenantId(),
                            dto.getApplicationId(),
                            version(dto.getExpectedVersion())));
            revocations.instance(dto.getTenantId(), dto.getId(), "INSTANCE_CHANGED");
            return dto.getId();
        }
        if (!"ACTIVE".equals(dto.getStatus())) throw new BizException(400, "首次开通必须选择ACTIVE");
        if (!store.queryForList(
                        "tenantApplicationServiceSelect07",
                        dto.getTenantId(),
                        dto.getApplicationId())
                .isEmpty()) throw new BizException(409, "已存在开通实例，请编辑原实例恢复或续期");
        if (dto.getAdminUserId() == null) throw new BizException(400, "首次开通请选择租户内的应用管理员");
        store.one("tenantApplicationServiceSelect04", dto.getAdminUserId(), dto.getTenantId());
        long id = IdWorker.getId(), role = IdWorker.getId();
        store.update(
                "tenantApplicationServiceUpdate10",
                id,
                dto.getTenantId(),
                dto.getApplicationId(),
                dto.getAccessPolicy(),
                dto.getEntryUrlOverride(),
                dto.getEffectiveAt(),
                dto.getExpireAt(),
                dto.getComment(),
                String.valueOf(boundary.userId()),
                String.valueOf(boundary.userId()));
        store.update(
                "tenantApplicationServiceUpdate11",
                role,
                dto.getTenantId(),
                dto.getApplicationId(),
                id);
        for (var permission :
                store.queryForList("tenantApplicationServiceSelect08", dto.getApplicationId())) {
            store.update(
                    "tenantApplicationServiceUpdate12",
                    IdWorker.getId(),
                    dto.getTenantId(),
                    dto.getApplicationId(),
                    id,
                    role,
                    permission.get("id"));
        }
        store.update(
                "tenantApplicationServiceUpdate13",
                IdWorker.getId(),
                dto.getTenantId(),
                dto.getApplicationId(),
                id,
                dto.getAdminUserId(),
                role);
        store.update(
                "tenantApplicationServiceUpdate14",
                IdWorker.getId(),
                dto.getTenantId(),
                id,
                dto.getAdminUserId(),
                boundary.userId());
        store.update("tenantApplicationServiceUpdate15", id);
        return id;
    }

    public List<UserApplicationVO> candidates(long tenant) {
        boundary.require("provision");
        return store.list(UserApplicationVO.class, "tenantApplicationServiceSelect06", tenant);
    }
}
