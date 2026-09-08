package cn.utopiabin.cloud.platform.mapper.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

  /** 初始化重名检查由目标租户限定，平台启动时没有登录上下文。 */
  @InterceptorIgnore(tenantLine = "true")
  List<Long> selectInitializationUserIdsForUpdate(
      @Param("tenantId") long tenantId,
      @Param("username") String username,
      @Param("includeDeleted") boolean includeDeleted);
}
