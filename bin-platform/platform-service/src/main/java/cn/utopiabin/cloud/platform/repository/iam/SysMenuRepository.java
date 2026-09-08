package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuListQuery;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * 系统菜单 Repository
 *
 * @since 1.0
 */
@Repository
public class SysMenuRepository extends BaseRepository<SysMenuMapper, SysMenu> {

  @Override
  protected String getNotFoundMessage() {
    return "菜单不存在";
  }

  /** 是否有子菜单 */
  public boolean hasChild(Long parentId) {
    return count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, parentId)) > 0;
  }

  /** 列表查询 */
  public List<SysMenu> list(SysMenuListQuery query) {
    var qw =
        new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort).orderByDesc(SysMenu::getId);
    if (query != null) {
      qw.like(StrUtil.isNotBlank(query.getName()), SysMenu::getName, query.getName())
          .eq(Objects.nonNull(query.getAvailable()), SysMenu::getAvailable, query.getAvailable());
    }
    return list(qw);
  }

  public List<SysMenu> listByPermissionCodes(List<String> permissionCodes) {
    List<String> codes = permissionCodes == null ? List.of() : permissionCodes;
    LambdaQueryWrapper<SysMenu> query =
        new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getAvailable, true)
            .orderByAsc(SysMenu::getSort)
            .orderByDesc(SysMenu::getId);
    if (!codes.contains("*")) {
      query.and(
          scope -> {
            scope.isNull(SysMenu::getPermission).or().eq(SysMenu::getPermission, "");
            if (!codes.isEmpty()) {
              scope.or().in(SysMenu::getPermission, codes);
            }
          });
    }
    return list(query);
  }

  public List<SysMenuVO> listViewsByPermissionCodes(List<String> permissionCodes) {
    return listByPermissionCodes(permissionCodes).stream()
        .map(menu -> menu.copyTo(SysMenuVO.class))
        .toList();
  }

  public void ensureInitialConsoleMenu(
      String name, String path, String icon, String permission, int sort) {
    // 外层初始化事务已锁定全局通配权限；保留既有路径，包括禁用和逻辑删除的菜单。
    if (!baseMapper.selectConsoleMenuIdsIncludingDeletedForUpdate(path).isEmpty()) {
      return;
    }
    SysMenu menu = new SysMenu();
    menu.setApplicationId(1L);
    menu.setType(2);
    menu.setName(name);
    menu.setPath(path);
    menu.setIcon(icon);
    menu.setPermission(permission);
    menu.setSort(sort);
    save(menu);
  }
}
