package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.dto.auth.PhoneRegisterDTO;
import cn.utopiabin.cloud.platform.model.enums.SmsScene;
import cn.utopiabin.cloud.platform.model.vo.iam.UserPermissionVO;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.util.JwtTokenService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Mock
    private SysUserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private JwtTokenProperties jwtTokenProperties;
    @Mock
    private RedisClient redisClient;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginSecurityProperties loginSecurityProperties;
    @Mock
    private PasswordValidator passwordValidator;
    @Mock
    private SmsService smsService;

    @Mock
    private cn.utopiabin.cloud.platform.service.application.SsoService ssoService;

    @InjectMocks
    private AuthService authService;

    @ParameterizedTest
    @ValueSource(strings = {"*", "platform:user:read"})
    void loginReturnsEffectivePermissionsEvenWithoutMenus(String permission) {
        var tenant = new Tenant();
        tenant.setId(7L);
        tenant.setAvailable(true);
        var user = user();
        var dto = new LoginDTO();
        dto.setTenantCode("test");
        dto.setUsername("test");
        dto.setPassword("test-only");
        when(tenantRepository.getByCode("test")).thenReturn(tenant);
        when(userRepository.getByTenantIdAndUsername(7L, "test")).thenReturn(user);
        when(passwordEncoder.matches("test-only", "encoded")).thenReturn(true);
        when(permissionService.getUserPermissions(10L)).thenReturn(permissions(permission));

        var result = authService.login(dto);

        assertEquals(java.util.List.of(permission), result.getPermissionCodes());
        assertEquals(java.util.List.of(), result.getMenus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "platform:role:read"})
    void currentUserReturnsPermissionsSeparatelyFromMenus(String permission) {
        UserContextHolder.set(UserContext.of("10", "test", "7", ""));
        when(userRepository.getOrThrow(10L)).thenReturn(user());
        when(permissionService.getUserPermissions(10L)).thenReturn(permissions(permission));

        var result = authService.currentUser();

        assertEquals(java.util.List.of(permission), result.getPermissionCodes());
        assertEquals(java.util.List.of(), result.getMenus());
    }

    private SysUser user() {
        var user = new SysUser();
        user.setId(10L);
        user.setTenantId(7L);
        user.setUsername("test");
        user.setPassword("encoded");
        user.setAvailable(true);
        return user;
    }

    private UserPermissionVO permissions(String permission) {
        return new UserPermissionVO(java.util.List.of(), java.util.List.of(), java.util.List.of(permission),
                java.util.List.of(), java.util.List.of(), java.util.List.of());
    }

    @Test
    void loginQueriesUserWithinResolvedTenantAndUsesTenantScopedFailureKey() {
        var dto = new LoginDTO();
        dto.setTenantCode("tenant-a");
        dto.setUsername("admin");
        dto.setPassword("wrong-password");

        var tenant = new Tenant();
        tenant.setId(7L);
        tenant.setAvailable(true);
        when(tenantRepository.getByCode("tenant-a")).thenReturn(tenant);
        when(redisClient.incr("platform:login:fail:7:admin")).thenReturn(1L);
        when(loginSecurityProperties.getFailCountWindowSeconds()).thenReturn(1800L);
        when(loginSecurityProperties.getMaxFailCount()).thenReturn(5);

        assertThrows(BizException.class, () -> authService.login(dto));

        verify(userRepository).getByTenantIdAndUsername(7L, "admin");
        verify(redisClient).hasKey("platform:login:lock:7:admin");
        verify(redisClient).expire("platform:login:fail:7:admin", java.time.Duration.ofSeconds(1800));
    }

    @Test
    void registerByPhoneConsumesRegisterCodeAndCreatesTenantUser() {
        var dto = new PhoneRegisterDTO();
        dto.setTenantCode("tenant-a");
        dto.setPhone("13800138000");
        dto.setCode("123456");
        dto.setPassword("Strong123");

        var tenant = new Tenant();
        tenant.setId(7L);
        tenant.setAvailable(true);
        when(tenantRepository.getByCode("tenant-a")).thenReturn(tenant);
        when(passwordEncoder.encode("Strong123")).thenReturn("encoded");
        doAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setId(10L);
            return true;
        }).when(userRepository).save(any(SysUser.class));
        when(permissionService.getUserPermissions(10L)).thenReturn(
                new UserPermissionVO(java.util.List.of(), java.util.List.of(), java.util.List.of(),
                        java.util.List.of(), java.util.List.of(), java.util.List.of()));
        when(ssoService.platformLogin(any(SysUser.class), any()))
                .thenReturn("token");

        var result = authService.registerByPhone(dto);

        assertEquals("token", result.getToken());
        verify(smsService).verifyAndConsume(7L, "13800138000", SmsScene.REGISTER, "123456");
        verify(passwordValidator).validate("Strong123");
    }
}
