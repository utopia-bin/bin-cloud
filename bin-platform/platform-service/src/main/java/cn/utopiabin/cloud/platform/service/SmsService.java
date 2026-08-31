package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.redis.RedisClient;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.annotation.TenantIgnore;
import cn.utopiabin.cloud.platform.config.SmsProperties;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.system.SysSmsSendLog;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.model.dto.sms.SmsCodeSendDTO;
import cn.utopiabin.cloud.platform.model.enums.SmsScene;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.system.SysSmsSendLogRepository;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import cn.utopiabin.cloud.platform.spi.sms.SmsSendCommand;
import cn.utopiabin.cloud.platform.spi.sms.SmsSendResult;
import cn.utopiabin.cloud.platform.spi.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 短信验证码编排服务；厂商发送能力由 {@link SmsSender} SPI 提供。 */
@Slf4j
@Service
public class SmsService {

    private static final String CODE_KEY_PREFIX = "platform:sms:code:";
    private static final String COOLDOWN_KEY_PREFIX = "platform:sms:cooldown:";
    private static final String ATTEMPT_KEY_PREFIX = "platform:sms:attempt:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisClient redisClient;
    private final TenantRepository tenantRepository;
    private final SysUserRepository userRepository;
    private final SysSmsSendLogRepository sendLogRepository;
    private final SmsProperties properties;
    private final Map<String, SmsSender> senders;

    public SmsService(RedisClient redisClient,
                      TenantRepository tenantRepository,
                      SysUserRepository userRepository,
                      SysSmsSendLogRepository sendLogRepository,
                      SmsProperties properties,
                      List<SmsSender> senderList) {
        this.redisClient = redisClient;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.sendLogRepository = sendLogRepository;
        this.properties = properties;
        this.senders = new LinkedHashMap<>();
        for (SmsSender sender : senderList) {
            String provider = normalizeProvider(sender.provider());
            if (StrUtil.isBlank(provider) || senders.putIfAbsent(provider, sender) != null) {
                throw new IllegalStateException("短信厂商标识为空或重复: " + provider);
            }
        }
    }

    /** 校验业务场景、执行频控并调用厂商发送短信。 */
    @TenantIgnore
    @cn.utopiabin.cloud.platform.annotation.OperateLog(module = "短信认证", action = "发送验证码",
            type = cn.utopiabin.cloud.platform.annotation.OperateType.AUTH, maskParams = true)
    public void sendVerificationCode(SmsCodeSendDTO dto) {
        String tenantCode = trim(dto.getTenantCode());
        String phone = trim(dto.getPhone());
        Tenant tenant = requireAvailableTenant(tenantCode);
        validateSceneUser(tenant.getId(), phone, dto.getScene());

        String cooldownKey = cooldownKey(tenant.getId(), phone, dto.getScene());
        if (!redisClient.setIfAbsent(cooldownKey, "1", properties.getSendInterval())) {
            throw biz(PlatformErrorCode.SMS_SEND_TOO_FREQUENT);
        }

        SmsSender sender = resolveSender();
        String provider = sender == null ? normalizeProvider(properties.getDefaultProvider()) : sender.provider();
        String templateCode = resolveTemplate(dto.getScene());
        if (sender == null) {
            saveLog(tenant.getId(), phone, dto.getScene(), provider, templateCode,
                    SmsSendResult.failure("PROVIDER_UNAVAILABLE", "未配置可用的短信发送器"));
            redisClient.delete(cooldownKey);
            throw biz(PlatformErrorCode.SMS_PROVIDER_UNAVAILABLE);
        }

        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        SmsSendResult result;
        try {
            result = sender.send(new SmsSendCommand(phone, templateCode, Map.of(
                    "code", code,
                    "minutes", String.valueOf(Math.max(1, properties.getCodeTtl().toMinutes())))));
            if (result == null) {
                result = SmsSendResult.failure("EMPTY_RESULT", "短信厂商未返回发送结果");
            }
        } catch (RuntimeException ex) {
            log.warn("短信厂商调用异常: tenantId={}, scene={}, provider={}, exceptionType={}",
                    tenant.getId(), dto.getScene(), sender.provider(), ex.getClass().getSimpleName());
            result = SmsSendResult.failure("PROVIDER_EXCEPTION", ex.getClass().getSimpleName());
        }

        saveLog(tenant.getId(), phone, dto.getScene(), sender.provider(), templateCode, result);
        if (!result.success()) {
            redisClient.delete(cooldownKey);
            throw biz(PlatformErrorCode.SMS_SEND_FAILED);
        }
        redisClient.delete(attemptKey(tenant.getId(), phone, dto.getScene()));
        redisClient.set(codeKey(tenant.getId(), phone, dto.getScene()), code, properties.getCodeTtl());
    }

    /** 匹配并原子消费一次性验证码。 */
    public void verifyAndConsume(Long tenantId, String phone, SmsScene scene, String code) {
        String normalizedPhone = trim(phone);
        String codeKey = codeKey(tenantId, normalizedPhone, scene);
        String attemptKey = attemptKey(tenantId, normalizedPhone, scene);
        if (StrUtil.isNotBlank(code) && redisClient.compareAndDelete(codeKey, code.trim())) {
            redisClient.delete(attemptKey);
            return;
        }

        Long attempts = redisClient.incr(attemptKey);
        long count = attempts == null ? 1L : attempts;
        if (count == 1L) {
            redisClient.expire(attemptKey, properties.getCodeTtl());
        }
        if (count >= Math.max(1, properties.getMaxVerifyAttempts())) {
            redisClient.delete(codeKey);
            redisClient.delete(attemptKey);
        }
        throw biz(PlatformErrorCode.SMS_CODE_ERROR);
    }

    private void validateSceneUser(Long tenantId, String phone, SmsScene scene) {
        boolean registered = userRepository.getByTenantIdAndPhone(tenantId, phone) != null;
        if (scene == SmsScene.REGISTER && registered) {
            throw biz(PlatformErrorCode.PHONE_DUPLICATE);
        }
        if (scene != SmsScene.REGISTER && !registered) {
            throw biz(PlatformErrorCode.PHONE_NOT_REGISTERED);
        }
    }

    private Tenant requireAvailableTenant(String tenantCode) {
        Tenant tenant = tenantRepository.getByCode(tenantCode);
        if (tenant == null) {
            throw biz(PlatformErrorCode.TENANT_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(tenant.getAvailable())) {
            throw biz(PlatformErrorCode.TENANT_DISABLED);
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw biz(PlatformErrorCode.TENANT_EXPIRED);
        }
        return tenant;
    }

    private SmsSender resolveSender() {
        String configured = normalizeProvider(properties.getDefaultProvider());
        if (StrUtil.isNotBlank(configured)) {
            return senders.get(configured);
        }
        return senders.size() == 1 ? senders.values().iterator().next() : null;
    }

    private String resolveTemplate(SmsScene scene) {
        String configured = properties.getTemplates().get(scene);
        return StrUtil.isNotBlank(configured) ? configured.trim() : scene.name();
    }

    private void saveLog(Long tenantId, String phone, SmsScene scene, String provider,
                         String templateCode, SmsSendResult result) {
        SysSmsSendLog entity = new SysSmsSendLog();
        entity.setTenantId(tenantId);
        entity.setPhone(phone);
        entity.setScene(scene.name());
        entity.setProvider(trim(provider));
        entity.setTemplateCode(templateCode);
        entity.setSuccess(result.success());
        entity.setRequestId(truncate(result.requestId(), 100));
        entity.setErrorCode(truncate(result.errorCode(), 100));
        entity.setErrorMessage(truncate(result.errorMessage(), 500));
        entity.setSendTime(new Date());
        sendLogRepository.save(entity);
    }

    private String codeKey(Long tenantId, String phone, SmsScene scene) {
        return CODE_KEY_PREFIX + tenantId + ":" + scene.name() + ":" + phone;
    }

    private String cooldownKey(Long tenantId, String phone, SmsScene scene) {
        return COOLDOWN_KEY_PREFIX + tenantId + ":" + scene.name() + ":" + phone;
    }

    private String attemptKey(Long tenantId, String phone, SmsScene scene) {
        return ATTEMPT_KEY_PREFIX + tenantId + ":" + scene.name() + ":" + phone;
    }

    private static String normalizeProvider(String provider) {
        return trim(provider).toLowerCase(Locale.ROOT);
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static BizException biz(PlatformErrorCode error) {
        return new BizException(error.getCode(), error.getMsg());
    }
}
