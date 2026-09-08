package cn.utopiabin.cloud.platform.mapper.tenant;

import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 租户 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

  /** 初始化包含已删除租户，防止静默重建或恢复历史租户。 */
  Tenant selectByCodeIncludingDeletedForUpdate(@Param("code") String code);
}
