package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysTenantApplication;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.TenantApplicationVO;
import cn.utopiabin.cloud.platform.repository.application.model.ApplicationAccessRecord;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 租户应用实例 Mapper。
 *
 * <p>跨应用操作由 Repository 显式限定租户、应用或实例，不能套用平台壳过滤。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TenantApplicationMapper extends BaseMapper<SysTenantApplication> {
  long tenantApplicationCount(
      @Param("query") ApplicationQuery query, @Param("tenantId") Long tenantId);

  List<TenantApplicationVO> tenantApplicationPage(
      @Param("query") ApplicationQuery query,
      @Param("tenantId") Long tenantId,
      @Param("offset") long offset,
      @Param("limit") int limit);

  List<TenantApplicationVO> tenantApplicationMine(@Param("tenantId") long tenantId);

  ApplicationAccessRecord selectUserAccess(
      @Param("userId") long userId,
      @Param("instanceId") long instanceId,
      @Param("tenantId") long tenantId);
}
