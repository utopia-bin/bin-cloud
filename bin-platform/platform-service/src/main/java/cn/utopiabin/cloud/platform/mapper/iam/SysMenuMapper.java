package cn.utopiabin.cloud.platform.mapper.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统菜单 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

  /** 初始导航不覆盖已禁用或已删除的既有路径。 */
  @InterceptorIgnore(tenantLine = "true")
  List<Long> selectConsoleMenuIdsIncludingDeletedForUpdate(@Param("path") String path);
}
