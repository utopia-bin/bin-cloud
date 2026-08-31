package cn.utopiabin.cloud.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 * <p>
 * 注册拦截器链 (顺序敏感):
 * <ol>
 *   <li>多租户隔离 —— 必须在分页之前，否则分页 COUNT 查询不携带租户条件</li>
 *   <li>乐观锁 —— 为 @Version 更新绑定原始版本号并递增新版本号</li>
 *   <li>分页插件 —— 放在拦截器链最后，执行物理分页 (自动追加 LIMIT)</li>
 * </ol>
 *
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class MyBatisPlusConfig {

    private final PlatformTenantLineHandler tenantLineHandler;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();
        // 1. 多租户 SQL 隔离 (必须在分页插件之前)
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new ConsoleApplicationLineHandler()));
        // 2. @Version 生成的更新 SQL 依赖此插件提供 MP_OPTLOCK_VERSION_ORIGINAL。
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 3. 分页插件最后执行
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
