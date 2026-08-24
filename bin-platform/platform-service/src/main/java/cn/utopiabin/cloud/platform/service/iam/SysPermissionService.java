package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import cn.utopiabin.cloud.platform.repository.iam.SysMenuRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysPermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRolePermissionRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SysPermissionService {
    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysMenuRepository menuRepository;
    private final PermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    public Long create(SysPermissionCreateDTO dto) {
        String code = dto.getCode().trim().toLowerCase();
        assertCodeUnique(code, null);
        var permission = dto.copyTo(SysPermission.class);
        permission.setName(dto.getName().trim());
        permission.setCode(code);
        permission.setDescription(StrUtil.defaultIfBlank(dto.getDescription(), ""));
        permission.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        permission.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        permissionRepository.save(permission);
        return permission.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysPermissionUpdateDTO dto) {
        var permission = permissionRepository.getOrThrow(dto.getId());
        if (!Objects.equals(permission.getVersion(), dto.getExpectedVersion())) {
            throw conflict();
        }
        String code = dto.getCode().trim().toLowerCase();
        assertCodeUnique(code, dto.getId());
        if (!permission.getCode().equals(code)
                && menuRepository.exists(SysMenu::getPermission, permission.getCode())) {
            throw new BizException(PlatformErrorCode.PERMISSION_IN_USE.getCode(),
                    "权限编码已被菜单引用，需先迁移菜单");
        }
        permission.setName(dto.getName().trim());
        permission.setCode(code);
        permission.setDescription(StrUtil.defaultIfBlank(dto.getDescription(), ""));
        permission.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(permission.getAvailable()));
        permission.setSort(Optional.ofNullable(dto.getSort()).orElse(permission.getSort()));
        if (!permissionRepository.updateById(permission)) {
            throw conflict();
        }
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        var permission = permissionRepository.getOrThrow(id);
        if (rolePermissionRepository.existsByPermissionId(id)
                || menuRepository.exists(SysMenu::getPermission, permission.getCode())) {
            throw new BizException(PlatformErrorCode.PERMISSION_IN_USE.getCode(),
                    PlatformErrorCode.PERMISSION_IN_USE.getMsg());
        }
        permissionRepository.removeById(id);
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    public SysPermissionVO get(Long id) {
        return permissionRepository.getOrThrow(id).copyTo(SysPermissionVO.class);
    }

    public List<SysPermissionVO> list() {
        return permissionRepository.listAll().stream()
                .map(p -> p.copyTo(SysPermissionVO.class))
                .toList();
    }

    private void assertCodeUnique(String code, Long excludeId) {
        if (permissionRepository.countByField(SysPermission::getCode, code, excludeId) > 0) {
            throw new BizException(PlatformErrorCode.PERMISSION_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.PERMISSION_CODE_DUPLICATE.getMsg());
        }
    }

    private BizException conflict() {
        return new BizException(PlatformErrorCode.PERMISSION_VERSION_CONFLICT.getCode(),
                PlatformErrorCode.PERMISSION_VERSION_CONFLICT.getMsg());
    }
}
