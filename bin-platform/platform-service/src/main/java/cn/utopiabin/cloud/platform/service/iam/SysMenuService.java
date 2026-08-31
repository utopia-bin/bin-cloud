package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.repository.iam.SysMenuRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import cn.utopiabin.cloud.platform.util.MenuTreeBuilder;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
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
    private final SysMenuMapper menuMapper;
    private final PermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:menu:create")
    @OperateLog(module = "菜单管理", action = "新增菜单", type = OperateType.CREATE, maskParams = true)
    public Long create(SysMenuCreateDTO dto) {
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

        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
        return menu.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:menu:update")
    @OperateLog(module = "菜单管理", action = "修改菜单", type = OperateType.UPDATE, maskParams = true)
    public void update(SysMenuUpdateDTO dto) {
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(PlatformErrorCode.MENU_PARENT_SELF.getCode(),
                    PlatformErrorCode.MENU_PARENT_SELF.getMsg());
        }

        var menu = menuRepository.getOrThrow(dto.getId());
        if (!java.util.Objects.equals(menu.getVersion(), dto.getExpectedVersion())) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(), "菜单已被修改，请刷新后重试");
        }
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
        if (!menuRepository.updateById(menu)) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(), "菜单已被修改，请刷新后重试");
        }

        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:menu:delete")
    @OperateLog(module = "菜单管理", action = "删除菜单", type = OperateType.DELETE, maskParams = true)
    public void remove(Long id) {
        menuRepository.getOrThrow(id);
        if (menuRepository.hasChild(id)) {
            throw new BizException(PlatformErrorCode.MENU_HAS_CHILDREN.getCode(),
                    PlatformErrorCode.MENU_HAS_CHILDREN.getMsg());
        }
        menuRepository.removeById(id);
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:menu:delete")
    @OperateLog(module = "菜单管理", action = "删除菜单", type = OperateType.DELETE, maskParams = true)
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            if (menuRepository.hasChild(id)) {
                throw new BizException(PlatformErrorCode.MENU_HAS_CHILDREN.getCode(),
                        "菜单ID=" + id + " " + PlatformErrorCode.MENU_HAS_CHILDREN.getMsg());
            }
        }
        menuRepository.removeByIds(dto.getIds());
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @RequirePermission("platform:menu:read")
    public SysMenuVO get(Long id) {
        return menuRepository.getOrThrow(id).copyTo(SysMenuVO.class);
    }

    @RequirePermission("platform:menu:read")
    public List<SysMenuVO> list(SysMenuListQuery query) {
        return menuRepository.list(query).stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();
    }

    @RequirePermission("platform:menu:read")
    public List<SysMenuTreeVO> tree(SysMenuListQuery query) {
        var all = menuRepository.list(query);
        return MenuTreeBuilder.build(all);
    }

    @RequirePermission("platform:menu:read")
    public List<SysMenuVO> listByPermissionCodes(List<String> permissionCodes) {
        List<String> codes = permissionCodes == null ? List.of() : permissionCodes;
        List<SysMenu> menus = menuMapper.selectMenusByPermissionCodes(codes, codes.contains("*"));
        return menus.stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();
    }
}
