package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.entity.iam.SysRolePermission;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationRoleMapper;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationRolePermissionMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationRoleDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationRoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/** 应用实例角色与角色权限关联数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationRoleRepository extends ApplicationRepositorySupport {

  private final ApplicationRoleMapper mapper;
  private final ApplicationRolePermissionMapper permissionMapper;

  public void requireRole(long roleId, long tenantId, long applicationId, long instanceId) {
    require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getId)
                .eq(SysRole::getId, roleId)
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getApplicationId, applicationId)
                .eq(SysRole::getTenantApplicationId, instanceId)
                .eq(SysRole::getAvailable, true)));
  }

  public SysRole getRole(long roleId, long tenantId, long applicationId, long instanceId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getId, roleId)
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getApplicationId, applicationId)
                .eq(SysRole::getTenantApplicationId, instanceId)));
  }

  public List<ApplicationRoleVO> listRoles(long tenantId, long instanceId) {
    return mapper
        .selectList(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getTenantApplicationId, instanceId)
                .orderByAsc(SysRole::getSort, SysRole::getId))
        .stream()
        .map(
            role -> {
              ApplicationRoleVO result = new ApplicationRoleVO();
              BeanUtils.copyProperties(role, result);
              result.setPermissionIds(listRolePermissionIds(tenantId, instanceId, role.getId()));
              return result;
            })
        .toList();
  }

  public List<Long> listRolePermissionIds(long tenantId, long instanceId, long roleId) {
    return permissionMapper
        .selectList(
            new LambdaQueryWrapper<SysRolePermission>()
                .select(SysRolePermission::getPermissionId)
                .eq(SysRolePermission::getTenantId, tenantId)
                .eq(SysRolePermission::getTenantApplicationId, instanceId)
                .eq(SysRolePermission::getRoleId, roleId))
        .stream()
        .map(SysRolePermission::getPermissionId)
        .toList();
  }

  public List<String> listRoleCodes(long tenantId, long userId, long instanceId) {
    return mapper.selectUserApplicationRoleCodes(tenantId, userId, instanceId);
  }

  public void insertRole(
      long roleId, long tenantId, long applicationId, long instanceId, ApplicationRoleDTO dto) {
    SysRole role = new SysRole();
    role.setId(roleId);
    role.setTenantId(tenantId);
    role.setApplicationId(applicationId);
    role.setTenantApplicationId(instanceId);
    role.setName(dto.getName());
    role.setCode(dto.getCode());
    role.setDataScope(dto.getDataScope());
    role.setAvailable(dto.isAvailable());
    role.setSort(dto.getSort());
    mapper.insert(role);
  }

  public int updateRole(
      long roleId, long tenantId, long instanceId, ApplicationRoleDTO dto, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysRole>()
            .set(SysRole::getName, dto.getName())
            .set(SysRole::getCode, dto.getCode())
            .set(SysRole::getDataScope, dto.getDataScope())
            .set(SysRole::getAvailable, dto.isAvailable())
            .set(SysRole::getSort, dto.getSort())
            .setSql("version = version + 1")
            .eq(SysRole::getId, roleId)
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getTenantApplicationId, instanceId)
            .eq(SysRole::getVersion, version));
  }

  public void replaceRolePermissions(
      long tenantId, long applicationId, long instanceId, long roleId, List<Long> permissionIds) {
    removeRolePermissions(tenantId, instanceId, roleId);
    for (Long permissionId : permissionIds) {
      grantRolePermission(tenantId, applicationId, instanceId, roleId, permissionId);
    }
  }

  public void removeRolePermissions(long tenantId, long instanceId, long roleId) {
    permissionMapper.delete(
        new LambdaQueryWrapper<SysRolePermission>()
            .eq(SysRolePermission::getTenantId, tenantId)
            .eq(SysRolePermission::getTenantApplicationId, instanceId)
            .eq(SysRolePermission::getRoleId, roleId));
  }

  public SysRole lockRoleForDelete(long roleId, long tenantId, long instanceId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getId, roleId)
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getTenantApplicationId, instanceId)
                .last("FOR UPDATE")));
  }

  public int removeRole(long roleId, long tenantId, long instanceId, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysRole>()
            .set(SysRole::getIsDelete, 1)
            .setSql("version = version + 1")
            .eq(SysRole::getId, roleId)
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getTenantApplicationId, instanceId)
            .eq(SysRole::getVersion, version));
  }

  public void insertAdministratorRole(
      long roleId, long tenantId, long applicationId, long instanceId) {
    SysRole role = new SysRole();
    role.setId(roleId);
    role.setTenantId(tenantId);
    role.setApplicationId(applicationId);
    role.setTenantApplicationId(instanceId);
    role.setName("应用管理员");
    role.setCode("app_admin");
    role.setDataScope(1);
    mapper.insert(role);
  }

  public void grantRolePermission(
      long tenantId, long applicationId, long instanceId, long roleId, long permissionId) {
    SysRolePermission relation = new SysRolePermission();
    relation.setId(IdWorker.getId());
    relation.setTenantId(tenantId);
    relation.setApplicationId(applicationId);
    relation.setTenantApplicationId(instanceId);
    relation.setRoleId(roleId);
    relation.setPermissionId(permissionId);
    permissionMapper.insert(relation);
  }

  public boolean permissionIsReferenced(long applicationId, long permissionId) {
    return permissionMapper.selectCount(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getApplicationId, applicationId)
                .eq(SysRolePermission::getPermissionId, permissionId))
        > 0;
  }
}
