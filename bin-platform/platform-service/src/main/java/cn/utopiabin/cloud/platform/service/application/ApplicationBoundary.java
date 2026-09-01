package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.flag;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.isWithin;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.number;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.time;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.repository.application.ApplicationAccessRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationBoundary {
    private static final long PLATFORM_TENANT_ID = 1L;

    private final ApplicationAccessRepository repository;

    public long userId() {
        try {
            return Long.parseLong(UserContextHolder.getUserId());
        } catch (Exception e) {
            throw new BizException(401, "请先登录");
        }
    }

    public long tenantId() {
        userId();
        try {
            return Long.parseLong(UserContextHolder.getTenantId());
        } catch (Exception e) {
            throw new BizException(401, "租户上下文缺失");
        }
    }

    public Long queryTenant(Long requested) {
        long own = tenantId();
        if (own == PLATFORM_TENANT_ID) {
            return requested;
        }
        if (requested != null && requested != own) {
            throw new BizException(403, "不能访问其他租户的应用数据");
        }
        return requested == null ? own : requested;
    }

    public Map<String, Object> instance(long id) {
        var row = repository.getInstance(id);
        queryTenant(number(row, "tenant_id"));
        return row;
    }

    public Map<String, Object> access(long tenant, long user, long instance) {
        var row = repository.getUserAccess(user, instance, tenant);
        var now = LocalDateTime.now();
        if (!flag(row, "tenant_available")
                || number(row, "tenant_deleted") != 0
                || !flag(row, "user_available")
                || number(row, "user_deleted") != 0
                || !isWithin(null, time(row, "tenant_expire"), now)
                || !"ENABLED".equals(row.get("product_status"))
                || !"ACTIVE".equals(row.get("status"))
                || !isWithin(time(row, "effective_at"), time(row, "expire_at"), now)) {
            throw new BizException(403, "应用、租户、用户或开通实例已停用、尚未生效或已到期");
        }
        if (!List.of("ALL", "ASSIGNED").contains(row.get("access_policy")))
            throw new BizException(403, "不支持的应用准入策略");
        if ("ASSIGNED".equals(row.get("access_policy"))) {
            var grant = repository.listUserGrants(tenant, instance, user);
            if (grant.isEmpty()
                    || !"ACTIVE".equals(grant.getFirst().get("status"))
                    || !isWithin(
                            time(grant.getFirst(), "effective_at"),
                            time(grant.getFirst(), "expire_at"),
                            now)) {
                throw new BizException(403, "尚未获得该应用的有效准入授权");
            }
            row.put("grant_expire", time(grant.getFirst(), "expire_at"));
        }
        return row;
    }
}
