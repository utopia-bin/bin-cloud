package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.repository.iam.SysMenuRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleMenuRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import cn.utopiabin.cloud.platform.util.MenuTreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 系统菜单服务
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuRepository menuRepository;
    private final SysRoleMenuRepository roleMenuRepository;
    private final SysMenuMapper menuMapper;
    private final PermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    public void create(SysMenuCreateDTO dto) {
        var menu = dto.copyTo(SysMenu.class);
        menu.setParentId(Optional.ofNullable(dto.getParentId()).orElse(0L));
        menu.setType(Optional.ofNullable(dto.getType()).orElse(2));
        menu.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        menu.setVisible(Optional.ofNullable(dto.getVisible()).orElse(true));
        menu.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        menu.setName(dto.getName().trim());
        menu.setPath(StrUtil.defaultIfBlank(dto.getPath(), ""));
        menu.setComponent(StrUtil.defaultIfBlank(dto.getComponent(), ""));
        menu.setIcon(StrUtil.defaultIfBlank(dto.getIcon(), ""));
        menu.setPermission(StrUtil.defaultIfBlank(dto.getPermission(), ""));
        menuRepository.save(menu);

        permissionService.evictAllUserPermissions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysMenuUpdateDTO dto) {
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(PlatformErrorCode.MENU_PARENT_SELF.getCode(),
                    PlatformErrorCode.MENU_PARENT_SELF.getMsg());
        }

        var menu = menuRepository.getOrThrow(dto.getId());
        menu.setParentId(Optional.ofNullable(dto.getParentId()).orElse(menu.getParentId()));
        menu.setType(Optional.ofNullable(dto.getType()).orElse(menu.getType()));
        menu.setName(StrUtil.defaultIfBlank(dto.getName(), menu.getName()));
        menu.setPath(StrUtil.defaultIfBlank(dto.getPath(), ""));
        menu.setComponent(StrUtil.defaultIfBlank(dto.getComponent(), ""));
        menu.setIcon(StrUtil.defaultIfBlank(dto.getIcon(), ""));
        menu.setPermission(StrUtil.defaultIfBlank(dto.getPermission(), ""));
        menu.setSort(Optional.ofNullable(dto.getSort()).orElse(menu.getSort()));
        menu.setVisible(Optional.ofNullable(dto.getVisible()).orElse(menu.getVisible()));
        menu.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(menu.getAvailable()));
        menuRepository.updateById(menu);

        permissionService.evictAllUserPermissions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        menuRepository.getOrThrow(id);
        if (menuRepository.hasChild(id)) {
            throw new BizException(PlatformErrorCode.MENU_HAS_CHILDREN.getCode(),
                    PlatformErrorCode.MENU_HAS_CHILDREN.getMsg());
        }
        roleMenuRepository.removeByMenuId(id);
        menuRepository.removeById(id);
        permissionService.evictAllUserPermissions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            if (menuRepository.hasChild(id)) {
                throw new BizException(PlatformErrorCode.MENU_HAS_CHILDREN.getCode(),
                        "菜单ID=" + id + " " + PlatformErrorCode.MENU_HAS_CHILDREN.getMsg());
            }
            roleMenuRepository.removeByMenuId(id);
        }
        menuRepository.removeByIds(dto.getIds());
        permissionService.evictAllUserPermissions();
    }

    public SysMenuVO get(Long id) {
        return menuRepository.getOrThrow(id).copyTo(SysMenuVO.class);
    }

    public List<SysMenuVO> list(SysMenuListQuery query) {
        return menuRepository.list(query).stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();
    }

    public List<SysMenuTreeVO> tree(SysMenuListQuery query) {
        var all = menuRepository.list(query);
        return MenuTreeBuilder.build(all);
    }

    public List<SysMenuVO> listByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<SysMenu> menus = menuMapper.selectMenusByRoleIds(roleIds);
        return menus.stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();
    }
}
