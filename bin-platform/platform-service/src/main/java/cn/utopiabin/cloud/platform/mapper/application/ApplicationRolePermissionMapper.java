package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.iam.SysRolePermission;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用角色权限关联 Mapper。
 *
 * <p>跨应用操作由 Repository 显式限定租户、应用或实例，不能套用平台壳过滤。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationRolePermissionMapper extends BaseMapper<SysRolePermission> {}
