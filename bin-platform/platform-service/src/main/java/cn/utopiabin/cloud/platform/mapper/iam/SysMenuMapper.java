package cn.utopiabin.cloud.platform.mapper.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统菜单 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据生效的权限码投影可见菜单。
     *
     * @param permissionCodes 权限码列表
     * @param allPermissions  是否拥有通配权限
     * @return 菜单列表（仅含启用的菜单）
     */
    List<SysMenu> selectMenusByPermissionCodes(@Param("permissionCodes") List<String> permissionCodes,
                                               @Param("allPermissions") boolean allPermissions);
}
