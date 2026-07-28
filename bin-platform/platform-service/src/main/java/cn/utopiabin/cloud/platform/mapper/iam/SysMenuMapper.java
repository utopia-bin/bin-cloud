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
     * 根据角色 ID 列表查询菜单列表 (JOIN sys_role_menu + sys_menu, 去重)
     *
     * @param roleIds 角色 ID 列表
     * @return 菜单列表 (仅含启用的菜单)
     */
    List<SysMenu> selectMenusByRoleIds(@Param("roleIds") List<Long> roleIds);
}
