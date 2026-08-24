package cn.utopiabin.cloud.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 为操作日志异步落库等场景提供专用线程池。
 * 拒绝策略为 CallerRuns: 队列满时由调用线程执行，保证任务不丢失。
 *
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("platformAsyncExecutor")
    public Executor platformAsyncExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("platform-async-");
        // 队列满时由调用线程执行，避免任务丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("平台异步线程池初始化完成: core=2, max=8, queue=500");
        return executor;
    }
}
