package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationResourceDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationResourceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/** 应用产品菜单资源数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationMenuRepository extends ApplicationRepositorySupport {

  private final ApplicationMenuMapper mapper;

  public List<ApplicationResourceVO> listMenus(long applicationId) {
    return mapper
        .selectList(
            new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getApplicationId, applicationId)
                .orderByAsc(SysMenu::getSort, SysMenu::getId))
        .stream()
        .map(
            menu -> {
              ApplicationResourceVO result = new ApplicationResourceVO();
              BeanUtils.copyProperties(menu, result);
              return result;
            })
        .toList();
  }

  public List<SysMenu> listAvailableMenus(long applicationId) {
    return mapper.selectList(
        new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getApplicationId, applicationId)
            .eq(SysMenu::getAvailable, true)
            .orderByAsc(SysMenu::getSort, SysMenu::getId));
  }

  public SysMenu getMenu(long id, long applicationId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getId, id)
                .eq(SysMenu::getApplicationId, applicationId)));
  }

  public boolean hasChildMenu(long applicationId, long menuId) {
    return mapper.selectCount(
            new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getApplicationId, applicationId)
                .eq(SysMenu::getParentId, menuId))
        > 0;
  }

  public boolean permissionIsReferenced(long applicationId, String code) {
    return mapper.selectCount(
            new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getApplicationId, applicationId)
                .eq(SysMenu::getPermission, code))
        > 0;
  }

  public void insertMenu(long id, long parentId, ApplicationResourceDTO dto) {
    SysMenu menu = new SysMenu();
    menu.setId(id);
    menu.setApplicationId(dto.getApplicationId());
    menu.setParentId(parentId);
    menu.setType(dto.getType());
    menu.setName(dto.getName());
    menu.setPath(dto.getPath());
    menu.setComponent(dto.getComponent());
    menu.setIcon(dto.getIcon());
    menu.setPermission(dto.getPermission());
    menu.setRouteName(dto.getRouteName());
    menu.setOpenMode(dto.getOpenMode());
    menu.setVisible(dto.isVisible());
    menu.setAvailable(dto.isAvailable());
    menu.setSort(dto.getSort());
    mapper.insert(menu);
  }

  public int updateMenu(long id, long parentId, ApplicationResourceDTO dto, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysMenu>()
            .set(SysMenu::getParentId, parentId)
            .set(SysMenu::getType, dto.getType())
            .set(SysMenu::getName, dto.getName())
            .set(SysMenu::getPath, dto.getPath())
            .set(SysMenu::getComponent, dto.getComponent())
            .set(SysMenu::getIcon, dto.getIcon())
            .set(SysMenu::getPermission, dto.getPermission())
            .set(SysMenu::getRouteName, dto.getRouteName())
            .set(SysMenu::getOpenMode, dto.getOpenMode())
            .set(SysMenu::getVisible, dto.isVisible())
            .set(SysMenu::getAvailable, dto.isAvailable())
            .set(SysMenu::getSort, dto.getSort())
            .setSql("version = version + 1")
            .eq(SysMenu::getId, id)
            .eq(SysMenu::getApplicationId, dto.getApplicationId())
            .eq(SysMenu::getVersion, version));
  }

  public int removeMenu(long id, long applicationId, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysMenu>()
            .set(SysMenu::getIsDelete, 1)
            .setSql("version = version + 1")
            .eq(SysMenu::getId, id)
            .eq(SysMenu::getApplicationId, applicationId)
            .eq(SysMenu::getVersion, version));
  }
}
