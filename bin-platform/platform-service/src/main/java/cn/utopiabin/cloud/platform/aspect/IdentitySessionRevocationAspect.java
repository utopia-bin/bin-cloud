package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserUpdateDTO;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantUpdateDTO;
import cn.utopiabin.cloud.platform.service.application.ApplicationRevocationService;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Identity lifecycle changes invalidate sessions even if an account is immediately re-enabled. */
@Aspect
@Component
@RequiredArgsConstructor
public class IdentitySessionRevocationAspect {
    private final ApplicationRevocationService revocations;

    @AfterReturning("execution(* cn.utopiabin.cloud.platform.service.iam.SysUserService.update(..)) || execution(* cn.utopiabin.cloud.platform.service.iam.SysUserService.enable(..)) || execution(* cn.utopiabin.cloud.platform.service.iam.SysUserService.remove(..)) || execution(* cn.utopiabin.cloud.platform.service.iam.SysUserService.batchDelete(..)) || execution(* cn.utopiabin.cloud.platform.service.iam.SysUserService.resetPassword(..))")
    public void userChanged(JoinPoint point) {
        long tenant=Long.parseLong(UserContextHolder.getTenantId());
        Object arg=point.getArgs()[0];
        if(arg instanceof BatchDeleteDTO batch) batch.getIds().forEach(id->TransactionAfterCommitExecutor.afterCommit(()->revocations.user(tenant,id,"USER_CHANGED")));
        else {
            long user=arg instanceof SysUserUpdateDTO dto?dto.getId():((Number)arg).longValue();
            TransactionAfterCommitExecutor.afterCommit(()->revocations.user(tenant,user,"USER_CHANGED"));
        }
    }

    @AfterReturning("execution(* cn.utopiabin.cloud.platform.service.tenant.TenantService.update(..)) || execution(* cn.utopiabin.cloud.platform.service.tenant.TenantService.enable(..)) || execution(* cn.utopiabin.cloud.platform.service.tenant.TenantService.remove(..))")
    public void tenantChanged(JoinPoint point) {
        Object arg=point.getArgs()[0];
        long tenant=arg instanceof TenantUpdateDTO dto?dto.getId():((Number)arg).longValue();
        TransactionAfterCommitExecutor.afterCommit(()->revocations.tenant(tenant,"TENANT_CHANGED"));
    }
}
