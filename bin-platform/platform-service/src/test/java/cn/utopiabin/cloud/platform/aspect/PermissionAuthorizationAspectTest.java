package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionAuthorizationAspectTest {
    private final PermissionService permissionService = mock(PermissionService.class);
    private final PermissionAuthorizationAspect aspect =
            new PermissionAuthorizationAspect(permissionService);

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void rejectsAnonymousCaller() {
        assertThrows(BizException.class,
                () -> aspect.authorize(required("platform:user:read")));
    }

    @Test
    void rejectsCallerWithoutPermission() {
        UserContextHolder.set(new UserContext("10", "alice", "1", List.of("TENANT_ADMIN")));
        when(permissionService.hasPermission(10L, "platform:user:read")).thenReturn(false);

        assertThrows(BizException.class,
                () -> aspect.authorize(required("platform:user:read")));
    }

    @Test
    void allowsCallerWithServerSidePermission() {
        UserContextHolder.set(new UserContext("10", "alice", "1", List.of()));
        when(permissionService.hasPermission(10L, "platform:user:read")).thenReturn(true);

        assertDoesNotThrow(() -> aspect.authorize(required("platform:user:read")));
    }

    private RequirePermission required(String code) {
        return new RequirePermission() {
            @Override
            public String value() {
                return code;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequirePermission.class;
            }
        };
    }
}
