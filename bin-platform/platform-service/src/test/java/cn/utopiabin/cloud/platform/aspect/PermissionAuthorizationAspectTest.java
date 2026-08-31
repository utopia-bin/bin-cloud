package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.api.impl.system.SysDictApiImpl;
import cn.utopiabin.cloud.platform.api.impl.system.SysParameterApiImpl;
import cn.utopiabin.cloud.platform.service.PermissionService;
import cn.utopiabin.cloud.platform.service.system.SysDictService;
import cn.utopiabin.cloud.platform.service.system.SysParameterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void dictionaryApiActuallyEnforcesPermissionThroughSpringProxy() {
        var service = mock(SysDictService.class);
        var factory = new AspectJProxyFactory(new SysDictApiImpl(service));
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);
        SysDictApiImpl api = factory.getProxy();
        UserContextHolder.set(new UserContext("10", "alice", "1", List.of()));

        assertThrows(BizException.class, () -> api.getDict(1L));
        verifyNoInteractions(service);
        when(permissionService.hasPermission(10L, "platform:dict:read")).thenReturn(true);
        assertDoesNotThrow(() -> api.getDict(1L));
        verify(service).getDict(1L);
    }

    @Test
    void parameterApiActuallyEnforcesPermissionThroughSpringProxy() {
        var service = mock(SysParameterService.class);
        var factory = new AspectJProxyFactory(new SysParameterApiImpl(service));
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);
        SysParameterApiImpl api = factory.getProxy();
        UserContextHolder.set(new UserContext("10", "alice", "1", List.of()));

        assertThrows(BizException.class, api::refreshCache);
        verifyNoInteractions(service);
        when(permissionService.hasPermission(10L, "platform:parameter:update")).thenReturn(true);
        assertDoesNotThrow(api::refreshCache);
        verify(service).refreshCache();
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
