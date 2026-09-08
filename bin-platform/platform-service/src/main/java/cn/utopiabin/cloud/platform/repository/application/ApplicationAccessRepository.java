package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.entity.application.SysUserApplication;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationMemberMapper;
import cn.utopiabin.cloud.platform.mapper.application.TenantApplicationMapper;
import cn.utopiabin.cloud.platform.repository.application.model.ApplicationAccessRecord;
import cn.utopiabin.cloud.platform.repository.application.model.ApplicationGrantRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/** 应用访问边界数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationAccessRepository {
  private final TenantApplicationMapper instanceMapper;
  private final ApplicationMemberMapper memberMapper;

  public ApplicationAccessRecord getInstance(long instanceId) {
    var instance = require(instanceMapper.selectById(instanceId));
    var result = new ApplicationAccessRecord();
    BeanUtils.copyProperties(instance, result);
    return result;
  }

  public ApplicationAccessRecord getUserAccess(long userId, long instanceId, long tenantId) {
    return require(instanceMapper.selectUserAccess(userId, instanceId, tenantId));
  }

  public List<ApplicationGrantRecord> listUserGrants(long tenantId, long instanceId, long userId) {
    return memberMapper
        .selectList(
            new LambdaQueryWrapper<SysUserApplication>()
                .select(
                    SysUserApplication::getId, SysUserApplication::getStatus,
                    SysUserApplication::getEffectiveAt, SysUserApplication::getExpireAt)
                .eq(SysUserApplication::getTenantId, tenantId)
                .eq(SysUserApplication::getTenantApplicationId, instanceId)
                .eq(SysUserApplication::getUserId, userId))
        .stream()
        .map(
            grant -> {
              var result = new ApplicationGrantRecord();
              BeanUtils.copyProperties(grant, result);
              return result;
            })
        .toList();
  }

  private <T> T require(T record) {
    if (record == null) {
      throw new BizException(404, "应用实例不存在或不属于当前授权范围");
    }
    return record;
  }
}
