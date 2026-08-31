package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/** Converts persistence conflicts to declared RPC business errors without exposing SQL or bound values. */
@Aspect
@Component
@Order(0)
public class ApplicationPersistenceExceptionAspect {
    @Around("execution(public * cn.utopiabin.cloud.platform.service.application.*Service.*(..))")
    public Object translate(ProceedingJoinPoint call) throws Throwable {
        try {
            return call.proceed();
        } catch (DuplicateKeyException e) {
            throw new BizException(409, "应用编码、服务标识或授权关系已存在，请刷新检查");
        } catch (ConcurrencyFailureException e) {
            throw new BizException(409, "数据正在被其他操作修改，请刷新后重试");
        } catch (DataIntegrityViolationException e) {
            throw new BizException(400, "数据不满足应用或租户关联约束，请检查字段长度、必填项及引用关系");
        }
    }
}
