package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用权限资源 Mapper。
 *
 * <p>跨应用操作由 Repository 显式限定租户、应用或实例，不能套用平台壳过滤。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationPermissionMapper extends BaseMapper<SysPermission> {
  List<String> selectUserApplicationPermissionCodes(
      @Param("applicationId") long applicationId,
      @Param("tenantId") long tenantId,
      @Param("instanceId") long instanceId,
      @Param("userId") long userId);
}
