package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationTenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 应用开通时的租户锁定数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationTenantRepository extends ApplicationRepositorySupport {

  private final ApplicationTenantMapper mapper;

  public Tenant lock(long tenantId) {
    // 开通事务必须锁定目标租户，防止禁用、过期调整与首次开通交错。
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<Tenant>().eq(Tenant::getId, tenantId).last("FOR UPDATE")));
  }
}
