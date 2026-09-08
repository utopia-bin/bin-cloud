package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysSsoSession;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.SsoSessionVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SSO会话 Mapper。
 *
 * <p>跨应用操作由 Repository 显式限定租户、应用或实例，不能套用平台壳过滤。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationSessionMapper extends BaseMapper<SysSsoSession> {
  long sessionCount(@Param("query") ApplicationQuery query, @Param("tenantId") Long tenantId);

  List<SsoSessionVO> sessionPage(
      @Param("query") ApplicationQuery query,
      @Param("tenantId") Long tenantId,
      @Param("offset") long offset,
      @Param("limit") int limit);
}
