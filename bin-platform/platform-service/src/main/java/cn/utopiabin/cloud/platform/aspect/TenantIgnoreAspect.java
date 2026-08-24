package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.platform.annotation.TenantIgnore;
import cn.utopiabin.cloud.platform.tenant.TenantIgnoreContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 租户忽略切面
 * <p>
 * 拦截 {@link TenantIgnore} 注解方法，方法执行期间开启租户过滤跳过开关，
 * 无论成功/异常均恢复状态，防止 ThreadLocal 泄漏。
 *
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
public class TenantIgnoreAspect {

    @Around("@annotation(tenantIgnore)")
    public Object around(ProceedingJoinPoint joinPoint, TenantIgnore tenantIgnore) throws Throwable {
        TenantIgnoreContext.enable();
        try {
            return joinPoint.proceed();
        } finally {
            TenantIgnoreContext.disable();
        }
    }
}
