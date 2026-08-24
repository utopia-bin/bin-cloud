package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.util.JwtTokenService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @InjectMocks
    private AuthService authService;

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
}
