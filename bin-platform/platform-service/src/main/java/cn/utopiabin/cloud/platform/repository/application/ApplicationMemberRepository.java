package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.application.SysUserApplication;
import cn.utopiabin.cloud.platform.entity.iam.SysUserRole;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationMemberMapper;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationUserRoleMapper;
import cn.utopiabin.cloud.platform.model.dto.application.UserGrantDTO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 应用实例成员与成员角色关联数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationMemberRepository extends ApplicationRepositorySupport {

  private final ApplicationMemberMapper mapper;
  private final ApplicationUserRoleMapper userRoleMapper;

  public List<UserApplicationVO> listMembers(long instanceId, long tenantId) {
    var rows = mapper.selectInstanceMembers(instanceId, tenantId);
    rows.forEach(row -> row.setRoleIds(listMemberRoleIds(tenantId, instanceId, row.getUserId())));
    return rows;
  }

  public List<Long> listMemberRoleIds(long tenantId, long instanceId, long userId) {
    return userRoleMapper
        .selectList(
            new LambdaQueryWrapper<SysUserRole>()
                .select(SysUserRole::getRoleId)
                .eq(SysUserRole::getTenantId, tenantId)
                .eq(SysUserRole::getTenantApplicationId, instanceId)
                .eq(SysUserRole::getUserId, userId))
        .stream()
        .map(SysUserRole::getRoleId)
        .toList();
  }

  public SysUserApplication lockMember(long tenantId, long instanceId, long userId) {
    // 实例锁由业务事务先获取，再锁定已有成员，避免空记录并发创建。
    return mapper.selectOne(
        new LambdaQueryWrapper<SysUserApplication>()
            .eq(SysUserApplication::getTenantId, tenantId)
            .eq(SysUserApplication::getTenantApplicationId, instanceId)
            .eq(SysUserApplication::getUserId, userId)
            .last("FOR UPDATE"));
  }

  public void insertMember(long tenantId, long instanceId, UserGrantDTO dto, long operatorId) {
    SysUserApplication member = new SysUserApplication();
    member.setId(IdWorker.getId());
    member.setTenantId(tenantId);
    member.setTenantApplicationId(instanceId);
    member.setUserId(dto.getUserId());
    member.setStatus(dto.getStatus());
    member.setEffectiveAt(dto.getEffectiveAt());
    member.setExpireAt(dto.getExpireAt());
    member.setGrantedBy(operatorId);
    member.setComment(dto.getComment());
    mapper.insert(member);
  }

  public int updateMember(UserGrantDTO dto, long operatorId, long id, long tenantId, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysUserApplication>()
            .set(SysUserApplication::getStatus, dto.getStatus())
            .set(SysUserApplication::getEffectiveAt, dto.getEffectiveAt())
            .set(SysUserApplication::getExpireAt, dto.getExpireAt())
            .set(SysUserApplication::getComment, dto.getComment())
            .set(SysUserApplication::getGrantedBy, operatorId)
            .setSql("granted_at = CURRENT_TIMESTAMP, version = version + 1")
            .eq(SysUserApplication::getId, id)
            .eq(SysUserApplication::getTenantId, tenantId)
            .eq(SysUserApplication::getTenantApplicationId, dto.getTenantApplicationId())
            .eq(SysUserApplication::getUserId, dto.getUserId())
            .eq(SysUserApplication::getVersion, version));
  }

  public void replaceMemberRoles(
      long tenantId, long applicationId, long instanceId, long userId, List<Long> roleIds) {
    userRoleMapper.delete(
        new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getTenantId, tenantId)
            .eq(SysUserRole::getApplicationId, applicationId)
            .eq(SysUserRole::getTenantApplicationId, instanceId)
            .eq(SysUserRole::getUserId, userId));
    for (Long roleId : roleIds) {
      assignRole(tenantId, applicationId, instanceId, userId, roleId);
    }
  }

  public void assignRole(
      long tenantId, long applicationId, long instanceId, long userId, long roleId) {
    SysUserRole relation = new SysUserRole();
    relation.setId(IdWorker.getId());
    relation.setTenantId(tenantId);
    relation.setApplicationId(applicationId);
    relation.setTenantApplicationId(instanceId);
    relation.setUserId(userId);
    relation.setRoleId(roleId);
    userRoleMapper.insert(relation);
  }

  public void removeRoleMembers(long tenantId, long instanceId, long roleId) {
    userRoleMapper.delete(
        new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getTenantId, tenantId)
            .eq(SysUserRole::getTenantApplicationId, instanceId)
            .eq(SysUserRole::getRoleId, roleId));
  }

  public void grantApplication(long tenantId, long instanceId, long userId, long operatorId) {
    SysUserApplication member = new SysUserApplication();
    member.setId(IdWorker.getId());
    member.setTenantId(tenantId);
    member.setTenantApplicationId(instanceId);
    member.setUserId(userId);
    member.setStatus("ACTIVE");
    member.setGrantedBy(operatorId);
    mapper.insert(member);
  }
}
