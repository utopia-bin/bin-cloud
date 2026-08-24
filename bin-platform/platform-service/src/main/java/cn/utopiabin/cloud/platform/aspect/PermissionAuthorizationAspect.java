package cn.utopiabin.cloud.platform.aspect;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 在服务端执行权限判定，前端菜单仅用于展示。 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class PermissionAuthorizationAspect {
    private final PermissionService permissionService;

    @Before("@annotation(requirePermission)")
    public void authorize(RequirePermission requirePermission) {
        String userId = UserContextHolder.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new BizException(PlatformErrorCode.UNAUTHORIZED.getCode(),
                    PlatformErrorCode.UNAUTHORIZED.getMsg());
        }
        final long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new BizException(PlatformErrorCode.UNAUTHORIZED.getCode(),
                    PlatformErrorCode.UNAUTHORIZED.getMsg());
        }
        if (!permissionService.hasPermission(parsedUserId, requirePermission.value())) {
            throw new BizException(PlatformErrorCode.FORBIDDEN.getCode(),
                    "缺少权限: " + requirePermission.value());
        }
    }
}
