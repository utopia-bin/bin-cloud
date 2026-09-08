package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysUserApplication;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用实例成员查询 Mapper。
 *
 * <p>跨应用操作由 Repository 显式限定租户、应用或实例，不能套用平台壳过滤。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationMemberMapper extends BaseMapper<SysUserApplication> {
  List<UserApplicationVO> selectInstanceMembers(
      @Param("instanceId") long instanceId, @Param("tenantId") long tenantId);
}
