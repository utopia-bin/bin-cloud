package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleMenuRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRoleRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 系统角色服务
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleRepository roleRepository;
    private final SysRoleMenuRepository roleMenuRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysMenuMapper menuMapper;
    private final PermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    public void create(SysRoleCreateDTO dto) {
        var code = dto.getCode().trim();
        if (roleRepository.countByField(SysRole::getCode, code, null) > 0) {
            throw new BizException(PlatformErrorCode.ROLE_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.ROLE_CODE_DUPLICATE.getMsg());
        }

        var role = dto.copyTo(SysRole.class);
        role.setName(dto.getName().trim());
        role.setCode(code);
        role.setDataScope(Optional.ofNullable(dto.getDataScope()).orElse(1));
        role.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        role.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        role.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        roleRepository.save(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleUpdateDTO dto) {
        var role = roleRepository.getOrThrow(dto.getId());
        var code = dto.getCode().trim();
        if (roleRepository.countByField(SysRole::getCode, code, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.ROLE_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.ROLE_CODE_DUPLICATE.getMsg());
        }

        role.setName(dto.getName().trim());
        role.setCode(code);
        role.setDataScope(Optional.ofNullable(dto.getDataScope()).orElse(role.getDataScope()));
        role.setSort(Optional.ofNullable(dto.getSort()).orElse(role.getSort()));
        role.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(role.getAvailable()));
        role.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        roleRepository.updateById(role);

        permissionService.evictAllUserPermissions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        roleRepository.getOrThrow(id);
        roleMenuRepository.removeByRoleId(id);
        userRoleRepository.removeByRoleId(id);
        roleRepository.removeById(id);
        permissionService.evictAllUserPermissions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            roleMenuRepository.removeByRoleId(id);
            userRoleRepository.removeByRoleId(id);
        }
        roleRepository.removeByIds(dto.getIds());
        permissionService.evictAllUserPermissions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id, Boolean available) {
        var role = roleRepository.getOrThrow(id);
        role.setAvailable(available);
        roleRepository.updateById(role);
        permissionService.evictAllUserPermissions();
    }

    public SysRoleVO get(Long id) {
        return roleRepository.getOrThrow(id).copyTo(SysRoleVO.class);
    }

    public PageResult<SysRoleVO> page(SysRolePageQuery query) {
        Page<SysRole> page = roleRepository.page(query);
        var records = page.getRecords().stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public List<SysRoleVO> list(SysRoleListQuery query) {
        return roleRepository.list(query).stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(SysRoleAssignMenusDTO dto) {
        roleRepository.getOrThrow(dto.getRoleId());
        roleMenuRepository.assignMenus(dto.getRoleId(), dto.getMenuIds());
        permissionService.evictAllUserPermissions();
    }

    public List<SysMenuVO> getMenus(Long roleId) {
        var menuIds = roleMenuRepository.getMenuIdsByRoleId(roleId);
        if (menuIds.isEmpty()) {
            return List.of();
        }
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .map(m -> m.copyTo(SysMenuVO.class))
                .toList();
    }

    public boolean existsByCode(String code) {
        return roleRepository.exists(SysRole::getCode, code);
    }
}
