package cn.utopiabin.cloud.platform.mapper.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
  List<SysPermission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

  /** 全局通配权限的幂等插入不修改已有记录，并借助唯一键串行化首次启动。 */
  @InterceptorIgnore(tenantLine = "true")
  int ensurePlatformWildcard(@Param("id") long id);

  /** 包含逻辑删除记录，交由初始化服务拒绝异常的通配权限配置。 */
  @InterceptorIgnore(tenantLine = "true")
  SysPermission lockPlatformWildcard();
}
