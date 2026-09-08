package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.isWithin;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.repository.application.ApplicationAccessRepository;
import cn.utopiabin.cloud.platform.repository.application.model.ApplicationAccessRecord;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

  public ApplicationAccessRecord instance(long id) {
    var row = repository.getInstance(id);
    queryTenant(row.getTenantId());
    return row;
  }

  public ApplicationAccessRecord access(long tenant, long user, long instance) {
    var row = repository.getUserAccess(user, instance, tenant);
    var now = LocalDateTime.now();
    if (!Boolean.TRUE.equals(row.getTenantAvailable())
        || row.getTenantDeleted() != 0
        || !Boolean.TRUE.equals(row.getUserAvailable())
        || row.getUserDeleted() != 0
        || !isWithin(null, row.getTenantExpire(), now)
        || !"ENABLED".equals(row.getProductStatus())
        || !"ACTIVE".equals(row.getStatus())
        || !isWithin(row.getEffectiveAt(), row.getExpireAt(), now)) {
      throw new BizException(403, "应用、租户、用户或开通实例已停用、尚未生效或已到期");
    }
    if (!List.of("ALL", "ASSIGNED").contains(row.getAccessPolicy()))
      throw new BizException(403, "不支持的应用准入策略");
    if ("ASSIGNED".equals(row.getAccessPolicy())) {
      var grant = repository.listUserGrants(tenant, instance, user);
      if (grant.isEmpty()
          || !"ACTIVE".equals(grant.getFirst().getStatus())
          || !isWithin(grant.getFirst().getEffectiveAt(), grant.getFirst().getExpireAt(), now)) {
        throw new BizException(403, "尚未获得该应用的有效准入授权");
      }
      row.setGrantExpire(grant.getFirst().getExpireAt());
    }
    return row;
  }
}
