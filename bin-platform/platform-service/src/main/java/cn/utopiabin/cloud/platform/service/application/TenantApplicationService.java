package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.isWithin;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireSingleChange;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireVersion;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.validateWindow;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.InstanceDTO;
import cn.utopiabin.cloud.platform.model.vo.application.TenantApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;
import cn.utopiabin.cloud.platform.repository.application.ApplicationCatalogRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationMemberRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationPermissionRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationRoleRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationTenantRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationUserRepository;
import cn.utopiabin.cloud.platform.repository.application.TenantApplicationRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantApplicationService {
  private final TenantApplicationRepository repository;
  private final ApplicationCatalogRepository catalog;
  private final ApplicationTenantRepository tenants;
  private final ApplicationUserRepository users;
  private final ApplicationPermissionRepository permissions;
  private final ApplicationRoleRepository roles;
  private final ApplicationMemberRepository members;
  private final ApplicationBoundary boundary;
  private final ApplicationRevocationService revocations;

  @RequirePermission("platform:application:provision")
  public PageResult<TenantApplicationVO> page(ApplicationQuery q) {
    return repository.page(q, boundary.queryTenant(q.getTenantId()));
  }

  @RequirePermission("platform:application:read")
  public List<TenantApplicationVO> mine() {
    long tenant = boundary.tenantId(), user = boundary.userId();
    var list = repository.listAvailable(tenant);
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
    if (!repository.consoleExists(tenantId)) {
      repository.insertConsole(tenantId);
    }
  }

  @Transactional
  @RequirePermission("platform:application:provision")
  @OperateLog(module = "应用开通", action = "开通或调整租户应用", type = OperateType.UPDATE, maskParams = true)
  public long save(InstanceDTO dto) {
    if (dto.getApplicationId() == 1) throw new BizException(400, "平台壳随租户自动开通，生命周期由租户管理控制");
    validateWindow(dto.getEffectiveAt(), dto.getExpireAt());
    SsoCrypto.navigation(dto.getEntryUrlOverride(), true);
    // 先锁应用产品再锁租户，与应用状态变更保持同一加锁顺序，避免并发死锁。
    var app = catalog.lock(dto.getApplicationId());
    var tenant = tenants.lock(dto.getTenantId());
    if ("ACTIVE".equals(dto.getStatus())
        && (!"ENABLED".equals(app.getStatus())
            || !Boolean.TRUE.equals(tenant.getAvailable())
            || !isWithin(null, tenant.getExpireTime(), LocalDateTime.now())))
      throw new BizException(400, "租户或应用不可用，不能开通或恢复");
    if (dto.getId() != null) {
      var old = repository.lockInstance(dto.getId(), dto.getTenantId(), dto.getApplicationId());
      if (!List.of("ACTIVE", "SUSPENDED", "CLOSED", "EXPIRED", "PENDING").contains(old.getStatus()))
        throw new BizException(400, "不支持的原始状态");
      requireSingleChange(
          repository.updateInstance(
              dto, boundary.userId(), requireVersion(dto.getExpectedVersion())));
      revocations.instance(dto.getTenantId(), dto.getId(), "INSTANCE_CHANGED");
      return dto.getId();
    }
    if (!"ACTIVE".equals(dto.getStatus())) throw new BizException(400, "首次开通必须选择ACTIVE");
    if (repository.instanceExists(dto.getTenantId(), dto.getApplicationId())) {
      throw new BizException(409, "已存在开通实例，请编辑原实例恢复或续期");
    }
    if (dto.getAdminUserId() == null) throw new BizException(400, "首次开通请选择租户内的应用管理员");
    users.requireAvailableUser(dto.getAdminUserId(), dto.getTenantId());
    long id = IdWorker.getId(), role = IdWorker.getId();
    long operatorId = boundary.userId();
    repository.insertInstance(id, dto, operatorId);
    roles.insertAdministratorRole(role, dto.getTenantId(), dto.getApplicationId(), id);
    for (var permission : permissions.listAvailableIds(dto.getApplicationId())) {
      roles.grantRolePermission(dto.getTenantId(), dto.getApplicationId(), id, role, permission);
    }
    members.assignRole(dto.getTenantId(), dto.getApplicationId(), id, dto.getAdminUserId(), role);
    members.grantApplication(dto.getTenantId(), id, dto.getAdminUserId(), operatorId);
    requireSingleChange(repository.activate(id, dto.getTenantId(), dto.getApplicationId()));
    return id;
  }

  @RequirePermission("platform:application:provision")
  public List<UserApplicationVO> candidates(long tenant) {
    boundary.queryTenant(tenant);
    return users.listCandidates(tenant);
  }
}
