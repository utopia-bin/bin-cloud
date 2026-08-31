package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleAssignPermissionsDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleCreateDTO;
import cn.utopiabin.cloud.platform.repository.iam.*;
import cn.utopiabin.cloud.platform.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RoleGrantBoundaryTest {
    private final SysRoleRepository roles = mock(SysRoleRepository.class);
    private final SysRolePermissionRepository grants = mock(SysRolePermissionRepository.class);
    private final SysPermissionRepository permissions = mock(SysPermissionRepository.class);
    private final PermissionService authorization = mock(PermissionService.class);
    private final SysRoleService service = new SysRoleService(roles, grants, permissions,
            mock(SysUserRoleRepository.class), mock(SysPermissionMapper.class), authorization);

    @AfterEach
    void cleanup() { UserContextHolder.clear(); }

    @Test
    void tenantAdministratorCannotGrantWildcardTheyDoNotOwn() {
        var context = new UserContext(); context.setUserId("42"); context.setTenantId("9");
        UserContextHolder.set(context);
        var role = new SysRole(); role.setId(5L); role.setTenantId(9L); role.setVersion(0);
        when(roles.getOrThrow(5L)).thenReturn(role);
        var permission = new SysPermission(); permission.setId(1L); permission.setCode("*"); permission.setAvailable(true);
        when(permissions.listByIds(List.of(1L))).thenReturn(List.of(permission));
        var dto = new SysRoleAssignPermissionsDTO(); dto.setRoleId(5L); dto.setExpectedVersion(0); dto.setPermissionIds(List.of(1L));
        assertThatThrownBy(() -> service.assignPermissions(dto)).isInstanceOf(BizException.class).hasMessageContaining("自身授权范围");
        verifyNoInteractions(grants);
    }

    @Test
    void unsupportedDepartmentScopeIsRejectedBeforePersistence() {
        var dto = new SysRoleCreateDTO(); dto.setDataScope(2);
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(BizException.class).hasMessageContaining("未启用部门");
        verifyNoInteractions(roles);
    }
}
