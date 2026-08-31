package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.service.system.SysOperateLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import org.springframework.beans.BeanWrapperImpl;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 * <p>
 * 拦截 {@link OperateLog} 注解方法，记录操作人/租户/模块/动作/参数摘要/结果/耗时，
 * 异步落库，日志失败不影响主流程。
 * <p>
 * 切面顺序 ({@code @Order(1)}): 位于事务拦截器之外 —— 方法返回即事务已提交，
 * 审计结果与数据变更一致；线程池饱和降级为同步执行时也不会混入业务事务。
 * <p>
 * 参数记录时对敏感字段 (password/secret/token) 自动脱敏；
 * {@code maskParams=true} 时完全屏蔽参数 (仅记录个数)。
 *
 * @since 1.0
 */
@Slf4j
@Order(1)
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    /** 参数摘要最大长度 */
    private static final int PARAMS_MAX_LENGTH = 1000;

    /** 异常消息最大长度 */
    private static final int ERROR_MSG_MAX_LENGTH = 1024;

    /** 敏感字段脱敏正则 (password/secret/token 等 JSON 字段) */
    private static final Pattern SENSITIVE_PATTERN =
            Pattern.compile("(\"(?:password|secret|token|oldPassword|newPassword)\"\\s*:\\s*\")([^\"]*)(\")");

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private final SysOperateLogService operateLogService;
    private final TenantRepository tenantRepository;

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        long start = System.currentTimeMillis();
        String error = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e instanceof BizException biz ? "业务错误码: " + biz.getCode() : e.getClass().getSimpleName();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                recordAsync(joinPoint, operateLog, cost, error, result);
            } catch (Exception e) {
                log.warn("操作日志记录失败(不影响业务): module={}, action={}",
                        operateLog.module(), operateLog.action(), e);
            }
        }
    }

    /**
     * 异步落库 (复制上下文快照到局部变量，避免异步线程读到已清理的 ThreadLocal)
     */
    private void recordAsync(ProceedingJoinPoint joinPoint, OperateLog operateLog, long cost, String error, Object result) {
        String userId = UserContextHolder.getUserId();
        String username = UserContextHolder.getUsername();
        String tenantId = UserContextHolder.getTenantId();
        if (result instanceof LoginResultVO login && login.getUser() != null) {
            userId = String.valueOf(login.getUser().getId());
            tenantId = String.valueOf(login.getUser().getTenantId());
            username = login.getUser().getUsername();
        }
        if (tenantId == null && joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] != null) {
            var argument = new BeanWrapperImpl(joinPoint.getArgs()[0]);
            if (argument.isReadableProperty("tenantCode")) {
                var code = argument.getPropertyValue("tenantCode");
                var tenant = code == null ? null : tenantRepository.getByCode(code.toString().trim());
                if (tenant != null) tenantId = tenant.getId().toString();
            }
        }

        // 上下文缺失场景 (如登录) 从方法参数提取操作人
        if (username == null || username.isBlank()) {
            String principal = resolvePrincipal(joinPoint, operateLog.principalSpel());
            if (principal != null && !principal.isBlank()) {
                username = principal;
            }
        }

        if (username != null && username.matches("1[0-9]{10}")) {
            username = username.substring(0, 3) + "****" + username.substring(7);
        }
        String method = buildMethod(joinPoint);
        String params = operateLog.maskParams()
                ? joinPoint.getArgs().length + " args (masked)"
                : buildParams(joinPoint);
        boolean success = error == null;
        String errorMsg = error == null ? null : truncate(error, ERROR_MSG_MAX_LENGTH);

        operateLogService.asyncRecord(
                operateLog.module(), operateLog.action(), operateLog.type().name(),
                method, params, success, errorMsg, cost, userId, username, tenantId, org.slf4j.MDC.get("traceId"));
    }

    /**
     * 解析操作人 SpEL 表达式 (失败静默返回 null)
     */
    private String resolvePrincipal(ProceedingJoinPoint joinPoint, String spel) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var method = signature.getMethod();
            var context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, joinPoint.getArgs(),
                    new DefaultParameterNameDiscoverer());
            return SPEL_PARSER.parseExpression(spel).getValue(context, String.class);
        } catch (Exception e) {
            log.debug("操作人 SpEL 解析失败: expr={}, error={}", spel, e.getMessage());
            return null;
        }
    }

    private String buildMethod(ProceedingJoinPoint joinPoint) {
        var signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 参数摘要: JSON 序列化 + 敏感字段脱敏 + 截断
     */
    private String buildParams(ProceedingJoinPoint joinPoint) {
        String repr;
        try {
            String json = JsonUtil.toJson(joinPoint.getArgs());
            // JsonUtil.toJson 失败时返回 null → 降级为 deepToString
            repr = json != null ? json : "[serialization unavailable]";
        } catch (Exception e) {
            repr = "[serialization unavailable]";
        }
        String masked = SENSITIVE_PATTERN.matcher(repr).replaceAll("$1***$3");
        return truncate(masked, PARAMS_MAX_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
