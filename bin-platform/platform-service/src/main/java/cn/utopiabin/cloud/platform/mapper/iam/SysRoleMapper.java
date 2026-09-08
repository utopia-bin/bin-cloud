package cn.utopiabin.cloud.platform.mapper.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统角色 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

  List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

  /** 一次性初始化标记包括禁用和逻辑删除角色，且显式限定平台壳。 */
  @InterceptorIgnore(tenantLine = "true")
  List<Long> selectConsoleRoleIdsIncludingDeletedForUpdate(
      @Param("tenantId") Long tenantId, @Param("code") String code);
}
