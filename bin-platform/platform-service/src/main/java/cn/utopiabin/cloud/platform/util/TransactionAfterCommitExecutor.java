package cn.utopiabin.cloud.platform.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后执行助手
 * <p>
 * 解决"缓存清理早于事务提交"的时序问题: 若在事务方法内直接清除缓存，
 * 提交前的并发请求可能重新加载旧数据写入缓存，造成脏缓存长期存在。
 * <p>
 * 通过本助手将缓存失效动作注册到事务提交后执行:
 * <ul>
 *   <li>事务提交 → 执行清理 (数据已变更，缓存需失效)</li>
 *   <li>事务回滚 → 跳过清理 (数据未变更，缓存仍有效)</li>
 *   <li>无事务上下文 → 立即执行</li>
 * </ul>
 *
 * @since 1.0
 */
public final class TransactionAfterCommitExecutor {

    private TransactionAfterCommitExecutor() {
    }

    /**
     * 在当前事务提交后执行动作；无事务时立即执行
     *
     * @param action 待执行动作 (如缓存失效)
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        try {
                            action.run();
                        } catch (Exception e) {
                            // 清理失败不影响业务结果，仅记录
                            org.slf4j.LoggerFactory.getLogger(TransactionAfterCommitExecutor.class)
                                    .warn("事务提交后动作执行失败", e);
                        }
                    }
                }
            });
        } else {
            action.run();
        }
    }
}
