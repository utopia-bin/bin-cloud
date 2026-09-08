package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用实例角色 Mapper。
 *
 * <p>跨应用操作由 Repository 显式限定租户、应用或实例，不能套用平台壳过滤。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationRoleMapper extends BaseMapper<SysRole> {
  List<String> selectUserApplicationRoleCodes(
      @Param("tenantId") long tenantId,
      @Param("userId") long userId,
      @Param("instanceId") long instanceId);
}
