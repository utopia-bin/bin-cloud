package cn.utopiabin.cloud.platform.mapper.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
