package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.entity.application.SysTenantApplication;
import cn.utopiabin.cloud.platform.mapper.application.TenantApplicationMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.InstanceDTO;
import cn.utopiabin.cloud.platform.model.vo.application.TenantApplicationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 租户应用实例数据仓库。 */
@Repository
@RequiredArgsConstructor
public class TenantApplicationRepository extends ApplicationRepositorySupport {

  private final TenantApplicationMapper mapper;

  public PageResult<TenantApplicationVO> page(ApplicationQuery query, Long tenantId) {
    int page = Math.max(1, query.getPage());
    int size = Math.max(1, Math.min(100, query.getSize()));
    return PageResult.of(
        page,
        size,
        mapper.tenantApplicationCount(query, tenantId),
        mapper.tenantApplicationPage(query, tenantId, (long) (page - 1) * size, size));
  }

  public List<TenantApplicationVO> listAvailable(long tenantId) {
    return mapper.tenantApplicationMine(tenantId);
  }

  public long countActiveInstances(long applicationId) {
    return mapper.selectCount(
        new LambdaQueryWrapper<SysTenantApplication>()
            .eq(SysTenantApplication::getApplicationId, applicationId)
            .ne(SysTenantApplication::getStatus, "CLOSED"));
  }

  public boolean consoleExists(long tenantId) {
    return instanceExists(tenantId, 1);
  }

  public void insertConsole(long tenantId) {
    SysTenantApplication instance = new SysTenantApplication();
    instance.setId(tenantId);
    instance.setTenantId(tenantId);
    instance.setApplicationId(1L);
    instance.setStatus("ACTIVE");
    instance.setAccessPolicy("ALL");
    instance.setOpenedAt(LocalDateTime.now());
    mapper.insert(instance);
  }

  public void lockInstance(long instanceId, long tenantId) {
    require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysTenantApplication>()
                .select(SysTenantApplication::getId)
                .eq(SysTenantApplication::getId, instanceId)
                .eq(SysTenantApplication::getTenantId, tenantId)
                .last("FOR UPDATE")));
  }

  public SysTenantApplication lockInstance(long instanceId, long tenantId, long applicationId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysTenantApplication>()
                .eq(SysTenantApplication::getId, instanceId)
                .eq(SysTenantApplication::getTenantId, tenantId)
                .eq(SysTenantApplication::getApplicationId, applicationId)
                .last("FOR UPDATE")));
  }

  public boolean instanceExists(long tenantId, long applicationId) {
    return mapper.selectCount(
            new LambdaQueryWrapper<SysTenantApplication>()
                .eq(SysTenantApplication::getTenantId, tenantId)
                .eq(SysTenantApplication::getApplicationId, applicationId))
        > 0;
  }

  public int updateInstance(InstanceDTO dto, long operatorId, int expectedVersion) {
    LambdaUpdateWrapper<SysTenantApplication> update =
        new LambdaUpdateWrapper<SysTenantApplication>()
            .set(SysTenantApplication::getStatus, dto.getStatus())
            .set(SysTenantApplication::getAccessPolicy, dto.getAccessPolicy())
            .set(SysTenantApplication::getEntryUrlOverride, dto.getEntryUrlOverride())
            .set(SysTenantApplication::getEffectiveAt, dto.getEffectiveAt())
            .set(SysTenantApplication::getExpireAt, dto.getExpireAt())
            .set(SysTenantApplication::getComment, dto.getComment())
            .set(SysTenantApplication::getModifyUser, String.valueOf(operatorId))
            .setSql("version = version + 1")
            .eq(SysTenantApplication::getId, dto.getId())
            .eq(SysTenantApplication::getTenantId, dto.getTenantId())
            .eq(SysTenantApplication::getApplicationId, dto.getApplicationId())
            .eq(SysTenantApplication::getVersion, expectedVersion);
    // 恢复时保留历史停用和关闭时间，仅在进入对应状态时更新该时间。
    if ("SUSPENDED".equals(dto.getStatus())) {
      update.setSql("suspended_at = CURRENT_TIMESTAMP");
    }
    if ("CLOSED".equals(dto.getStatus())) {
      update.setSql("closed_at = CURRENT_TIMESTAMP");
    }
    return mapper.update(null, update);
  }

  public void insertInstance(long instanceId, InstanceDTO dto, long operatorId) {
    SysTenantApplication instance = new SysTenantApplication();
    instance.setId(instanceId);
    instance.setTenantId(dto.getTenantId());
    instance.setApplicationId(dto.getApplicationId());
    // 管理员角色与授权全部写入成功后，才在同一事务中激活实例。
    instance.setStatus("PENDING");
    instance.setAccessPolicy(dto.getAccessPolicy());
    instance.setEntryUrlOverride(dto.getEntryUrlOverride());
    instance.setOpenedAt(LocalDateTime.now());
    instance.setEffectiveAt(dto.getEffectiveAt());
    instance.setExpireAt(dto.getExpireAt());
    instance.setComment(dto.getComment());
    instance.setCreateUser(String.valueOf(operatorId));
    instance.setModifyUser(String.valueOf(operatorId));
    mapper.insert(instance);
  }

  public int activate(long instanceId, long tenantId, long applicationId) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysTenantApplication>()
            .set(SysTenantApplication::getStatus, "ACTIVE")
            .setSql("version = version + 1")
            .eq(SysTenantApplication::getId, instanceId)
            .eq(SysTenantApplication::getTenantId, tenantId)
            .eq(SysTenantApplication::getApplicationId, applicationId)
            .eq(SysTenantApplication::getStatus, "PENDING"));
  }
}
