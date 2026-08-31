package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.service.AuthService;
import cn.utopiabin.cloud.platform.service.system.SysOperateLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OperateLogAspectTest {
    private final SysOperateLogService logs = mock(SysOperateLogService.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final OperateLogAspect aspect = new OperateLogAspect(logs, tenants);

    @AfterEach
    void cleanup() { UserContextHolder.clear(); }

    private ProceedingJoinPoint loginCall() throws Exception {
        var dto = new LoginDTO();
        dto.setTenantCode("fixture");
        dto.setUsername("test_user");
        dto.setPassword("never-write-this-password");
        var call = mock(ProceedingJoinPoint.class);
        var signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(AuthService.class.getMethod("login", LoginDTO.class));
        when(signature.getDeclaringType()).thenReturn(AuthService.class);
        when(signature.getName()).thenReturn("login");
        when(call.getSignature()).thenReturn(signature);
        when(call.getArgs()).thenReturn(new Object[]{dto});
        return call;
    }

    private OperateLog annotation() throws Exception {
        return AuthService.class.getMethod("login", LoginDTO.class).getAnnotation(OperateLog.class);
    }

    @Test
    void successfulLoginUsesReturnedIdentityEvenWithoutThreadContext() throws Throwable {
        var call = loginCall();
        var user = new SysUserVO();
        user.setId(42L); user.setTenantId(9L); user.setUsername("test_user");
        var result = new LoginResultVO(); result.setUser(user);
        when(call.proceed()).thenReturn(result);
        assertThat(aspect.around(call, annotation())).isSameAs(result);
        verify(logs).asyncRecord(eq("认证管理"), eq("用户登录"), eq("AUTH"), eq("AuthService.login"),
                eq("1 args (masked)"), eq(true), isNull(), anyLong(), eq("42"), eq("test_user"), eq("9"), isNull());
    }

    @Test
    void failedLoginIsAssignedToTenantAndRethrowsOriginalError() throws Throwable {
        var call = loginCall();
        var tenant = new Tenant(); tenant.setId(9L);
        when(tenants.getByCode("fixture")).thenReturn(tenant);
        var failure = new BizException(400, "登录失败");
        when(call.proceed()).thenThrow(failure);
        assertThatThrownBy(() -> aspect.around(call, annotation())).isSameAs(failure);
        verify(logs).asyncRecord(anyString(), anyString(), eq("AUTH"), anyString(), eq("1 args (masked)"),
                eq(false), eq("业务错误码: 400"), anyLong(), isNull(), eq("test_user"), eq("9"), isNull());
    }

    @Test
    void auditFailureDoesNotReplaceBusinessResult() throws Throwable {
        var call = loginCall();
        when(call.proceed()).thenReturn("ok");
        doThrow(new IllegalStateException("audit unavailable")).when(logs).asyncRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), nullable(String.class),
                anyLong(), nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class));
        assertThat(aspect.around(call, annotation())).isEqualTo("ok");
    }
}
