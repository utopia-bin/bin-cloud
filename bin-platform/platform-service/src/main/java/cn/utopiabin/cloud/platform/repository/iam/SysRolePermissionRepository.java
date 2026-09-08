package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysRolePermission;
import cn.utopiabin.cloud.platform.mapper.iam.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SysRolePermissionRepository
    extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> {

  public void replace(Long tenantId, Long roleId, List<Long> permissionIds) {
    removeByRoleId(roleId);
    if (permissionIds == null || permissionIds.isEmpty()) {
      return;
    }
    saveBatch(
        permissionIds.stream()
            .distinct()
            .map(
                permissionId -> {
                  var relation = new SysRolePermission();
                  relation.setTenantId(tenantId);
                  relation.setTenantApplicationId(tenantId);
                  relation.setRoleId(roleId);
                  relation.setPermissionId(permissionId);
                  return relation;
                })
            .toList());
  }

  public List<Long> getPermissionIds(Long roleId) {
    return list(
            new LambdaQueryWrapper<SysRolePermission>()
                .select(SysRolePermission::getPermissionId)
                .eq(SysRolePermission::getRoleId, roleId))
        .stream()
        .map(SysRolePermission::getPermissionId)
        .toList();
  }

  public void removeByRoleId(Long roleId) {
    remove(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
  }

  public boolean existsByPermissionId(Long permissionId) {
    return count(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getPermissionId, permissionId))
        > 0;
  }

  public void grantConsolePermissions(long tenantId, long roleId, List<Long> permissionIds) {
    for (Long permissionId : permissionIds) {
      SysRolePermission relation = new SysRolePermission();
      relation.setTenantId(tenantId);
      relation.setApplicationId(1L);
      relation.setTenantApplicationId(tenantId);
      relation.setRoleId(roleId);
      relation.setPermissionId(permissionId);
      save(relation);
    }
  }
}
