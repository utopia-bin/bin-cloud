package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationUserMapper;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 应用授权和登录所需的租户用户数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationUserRepository extends ApplicationRepositorySupport {

  private final ApplicationUserMapper mapper;

  public void requireUser(long userId, long tenantId) {
    require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .eq(SysUser::getId, userId)
                .eq(SysUser::getTenantId, tenantId)));
  }

  public void requireAvailableUser(long userId, long tenantId) {
    require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .eq(SysUser::getId, userId)
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getAvailable, true)));
  }

  public List<UserApplicationVO> listCandidates(long tenantId) {
    return mapper
        .selectList(
            new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername)
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getAvailable, true)
                .orderByAsc(SysUser::getUsername))
        .stream()
        .map(
            user -> {
              UserApplicationVO result = new UserApplicationVO();
              result.setUserId(user.getId());
              result.setUsername(user.getUsername());
              return result;
            })
        .toList();
  }

  public void updateLastLogin(long userId, long tenantId) {
    mapper.update(
        null,
        new LambdaUpdateWrapper<SysUser>()
            .setSql("last_login_at = CURRENT_TIMESTAMP")
            .eq(SysUser::getId, userId)
            .eq(SysUser::getTenantId, tenantId));
  }
}
