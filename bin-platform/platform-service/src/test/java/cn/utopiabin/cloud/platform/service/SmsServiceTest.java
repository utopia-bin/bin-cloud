package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.platform.config.SmsProperties;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.system.SysSmsSendLog;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.model.dto.sms.SmsCodeSendDTO;
import cn.utopiabin.cloud.platform.model.enums.SmsScene;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.system.SysSmsSendLogRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.spi.sms.SmsSendResult;
import cn.utopiabin.cloud.platform.spi.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock private RedisClient redisClient;
    @Mock private TenantRepository tenantRepository;
    @Mock private SysUserRepository userRepository;
    @Mock private SysSmsSendLogRepository sendLogRepository;
    @Mock private SmsSender sender;

    private SmsProperties properties;
    private Tenant tenant;
    private SmsCodeSendDTO dto;

    @BeforeEach
    void setUp() {
        properties = new SmsProperties();
        tenant = new Tenant();
        tenant.setId(7L);
        tenant.setAvailable(true);
        dto = new SmsCodeSendDTO();
        dto.setTenantCode("tenant-a");
        dto.setPhone("13800138000");
        dto.setScene(SmsScene.REGISTER);
    }

    @Test
    void successfulSendStoresCodeInRedisAndPersistsAuditLog() {
        when(tenantRepository.getByCode("tenant-a")).thenReturn(tenant);
        when(sender.provider()).thenReturn("tencent");
        when(redisClient.setIfAbsent(any(), eq("1"), eq(Duration.ofSeconds(60)))).thenReturn(true);
        when(sender.send(any())).thenReturn(SmsSendResult.success("request-1"));
        SmsService service = service(List.of(sender));

        service.sendVerificationCode(dto);

        verify(redisClient).set(eq("platform:sms:code:7:REGISTER:13800138000"),
                org.mockito.ArgumentMatchers.argThat(value -> value instanceof String code
                        && code.matches("\\d{6}")),
                eq(Duration.ofMinutes(5)));
        ArgumentCaptor<SysSmsSendLog> logCaptor = ArgumentCaptor.forClass(SysSmsSendLog.class);
        verify(sendLogRepository).save(logCaptor.capture());
        SysSmsSendLog log = logCaptor.getValue();
        assertTrue(log.getSuccess());
        assertEquals("tencent", log.getProvider());
        assertEquals("request-1", log.getRequestId());
    }

    @Test
    void missingProviderPersistsFailureAndReleasesCooldown() {
        when(tenantRepository.getByCode("tenant-a")).thenReturn(tenant);
        when(redisClient.setIfAbsent(any(), eq("1"), eq(Duration.ofSeconds(60)))).thenReturn(true);
        SmsService service = service(List.of());

        BizException exception = assertThrows(BizException.class,
                () -> service.sendVerificationCode(dto));

        assertEquals(PlatformErrorCode.SMS_PROVIDER_UNAVAILABLE.getCode(), exception.getCode());
        verify(redisClient).delete("platform:sms:cooldown:7:REGISTER:13800138000");
        ArgumentCaptor<SysSmsSendLog> logCaptor = ArgumentCaptor.forClass(SysSmsSendLog.class);
        verify(sendLogRepository).save(logCaptor.capture());
        assertEquals("PROVIDER_UNAVAILABLE", logCaptor.getValue().getErrorCode());
    }

    @Test
    void validCodeIsConsumedAtomically() {
        SmsService service = service(List.of());
        when(redisClient.compareAndDelete("platform:sms:code:7:LOGIN:13800138000", "123456"))
                .thenReturn(true);

        service.verifyAndConsume(7L, "13800138000", SmsScene.LOGIN, "123456");

        verify(redisClient).delete("platform:sms:attempt:7:LOGIN:13800138000");
    }

    private SmsService service(List<SmsSender> senders) {
        return new SmsService(redisClient, tenantRepository, userRepository,
                sendLogRepository, properties, senders);
    }
}
