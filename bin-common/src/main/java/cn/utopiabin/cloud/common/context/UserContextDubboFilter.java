package cn.utopiabin.cloud.common.context;

import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.common.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo 用户上下文透传过滤器
 * <p>
 * 在 Dubbo RPC 调用边界自动传递和恢复 {@link UserContext}:
 * <ul>
 *   <li><b>Consumer 端</b>: 从当前线程的 {@link UserContextHolder} 读取上下文, 序列化后写入 RpcContext Attachment</li>
 *   <li><b>Provider 端</b>: 从 RpcContext Attachment 读取上下文, 反序列化后写入当前线程的 {@link UserContextHolder}</li>
 * </ul>
 * <p>
 * 通过 Dubbo SPI {@code @Activate} 自动激活, 无需手动配置。
 *
 * @since 1.0.0
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class UserContextDubboFilter implements Filter {

    /** RpcContext Attachment 中存储 UserContext 的 Key */
    private static final String ATTACHMENT_KEY = "USER_CONTEXT";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String side = RpcContext.getServiceContext().isProviderSide()
                ? CommonConstants.PROVIDER
                : CommonConstants.CONSUMER;

        if (CommonConstants.CONSUMER.equals(side)) {
            // Consumer 端: 将当前线程的 UserContext 携带到远程调用
            UserContext context = UserContextHolder.get();
            if (context != null && context.isValid()) {
                String contextJson = JsonUtil.toJson(context);
                // Dubbo 3.x: 存取 String 值统一使用 setAttachment/getAttachment, 避免类型不一致
                invocation.setAttachment(ATTACHMENT_KEY, contextJson);
                log.debug("Dubbo Consumer: 用户上下文已携带 → userId={}", context.getUserId());
            }
        } else {
            // Provider 端: 从 Attachment 恢复 UserContext
            String contextJson = invocation.getAttachment(ATTACHMENT_KEY);
            if (StrUtil.isNotBlank(contextJson)) {
                try {
                    UserContext context = JsonUtil.toObject(contextJson, UserContext.class);
                    if (context != null && context.isValid()) {
                        UserContextHolder.set(context);
                        log.debug("Dubbo Provider: 用户上下文已恢复 → userId={}", context.getUserId());
                    }
                } catch (Exception e) {
                    log.warn("Dubbo Provider: 用户上下文反序列化失败, 将忽略", e);
                }
            }
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            if (CommonConstants.PROVIDER.equals(side)) {
                // Provider 端执行完毕后清理, 防止线程池复用串上下文
                UserContextHolder.clear();
            }
        }
    }
}
