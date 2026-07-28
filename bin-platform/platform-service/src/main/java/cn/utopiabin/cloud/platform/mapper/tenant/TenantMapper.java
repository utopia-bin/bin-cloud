package cn.utopiabin.cloud.platform.mapper.tenant;

import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
