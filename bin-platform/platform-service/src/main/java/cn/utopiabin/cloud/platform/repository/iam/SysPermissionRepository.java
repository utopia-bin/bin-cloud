package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class SysPermissionRepository extends BaseRepository<SysPermissionMapper, SysPermission> {
  @Override
  protected String getNotFoundMessage() {
    return "权限资源不存在";
  }

  public List<SysPermission> listAll() {
    return list(
        new LambdaQueryWrapper<SysPermission>()
            .orderByAsc(SysPermission::getSort)
            .orderByAsc(SysPermission::getCode));
  }

  public List<SysPermission> listByRoleIds(List<Long> roleIds) {
    return roleIds == null || roleIds.isEmpty() ? List.of() : baseMapper.selectByRoleIds(roleIds);
  }

  public List<SysPermissionVO> listViewsByRoleIds(List<Long> roleIds) {
    return listByRoleIds(roleIds).stream()
        .map(permission -> permission.copyTo(SysPermissionVO.class))
        .toList();
  }

  public SysPermission ensureAndLockPlatformWildcard() {
    baseMapper.ensurePlatformWildcard(IdWorker.getId());
    return baseMapper.lockPlatformWildcard();
  }

  public List<Long> listAvailableConsolePermissionIds(Set<String> codes) {
    if (codes == null || codes.isEmpty()) {
      return List.of();
    }
    return list(
            new LambdaQueryWrapper<SysPermission>()
                .select(SysPermission::getId)
                .eq(SysPermission::getApplicationId, 1L)
                .isNull(SysPermission::getTenantId)
                .eq(SysPermission::getAvailable, true)
                .in(SysPermission::getCode, codes))
        .stream()
        .map(SysPermission::getId)
        .toList();
  }
}
