package cn.utopiabin.cloud.common.context;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.utils.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文过滤器 —— 从请求头自动提取用户信息并存入 {@link UserContextHolder}
 * <p>
 * 在每次 HTTP 请求到达时:
 * <ol>
 *   <li>从 X-User-Id / X-User-Name / X-Tenant-Id / X-User-Roles 请求头中读取用户信息</li>
 *   <li>构建 UserContext 并写入 ThreadLocal</li>
 *   <li>请求结束后在 finally 中清理 ThreadLocal, 防止内存泄漏</li>
 * </ol>
 * <p>
 * 注: 该过滤器依赖网关层 JwtAuthFilter 已注入上述请求头,
 * 若请求头为空则跳过设置, 下游业务需判空处理。
 *
 * @since 1.0.0
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@SuppressWarnings("NullableProblems")
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
            if (StrUtil.isNotBlank(userId)) {
                UserContext context = UserContext.of(
                        userId,
                        request.getHeader(CommonConstants.HEADER_USER_NAME),
                        request.getHeader(CommonConstants.HEADER_TENANT_ID),
                        request.getHeader(CommonConstants.HEADER_USER_ROLES)
                );
                UserContextHolder.set(context);
                log.debug("用户上下文已设置: userId={}, tenantId={}, path={}",
                        userId, context.getTenantId(), request.getRequestURI());
            }
            filterChain.doFilter(request, response);
        } finally {
            // 无论请求是否携带用户信息, 都必须清理, 防止线程池复用导致串上下文
            UserContextHolder.clear();
        }
    }
}
