package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.aspect.ApplicationPersistenceExceptionAspect;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.assertj.core.api.Assertions.*;

class ApplicationSpringWiringTest {
    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement
    static class Infrastructure { }

    @Test
    void transactionalServicesWorkThroughSpringProxiesAndReturnSafeRpcConflicts() throws Exception {
        var fixture = new ApplicationFixture();
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Infrastructure.class, ApplicationPersistenceExceptionAspect.class);
            context.registerBean(org.springframework.jdbc.core.JdbcTemplate.class, () -> fixture.jdbc);
            context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(fixture.jdbc.getDataSource()));
            context.registerBean(cn.utopiabin.cloud.platform.service.PermissionService.class, () -> fixture.permissions);
            context.registerBean(ApplicationStore.class, () -> fixture.store);
            context.register(ApplicationBoundary.class, SsoAuditService.class,
                    ApplicationRevocationService.class, ApplicationCatalogService.class);
            context.refresh();
            var service = context.getBean(ApplicationCatalogService.class);
            var dto = new ApplicationDTO();
            dto.setCode("fixture-app");dto.setName("Fixture");dto.setServiceId("fixture-service");
            dto.setEntryUrl("/fixture");dto.setSsoEnabled(false);
            long id = service.save(dto);
            assertThat(service.get(id).getCode()).isEqualTo("fixture-app");
            assertThatThrownBy(() -> service.save(dto)).isInstanceOf(BizException.class)
                    .hasMessageContaining("已存在").hasMessageNotContaining("INSERT").hasMessageNotContaining("SQL");
            var secret = service.rotate(id, 0);
            assertThat(secret.getClientSecret()).hasSize(43);
            assertThat(service.get(id).isClientConfigured()).isTrue();
            assertThat(fixture.jdbc.queryForObject("SELECT client_secret_hash FROM sys_application WHERE id=?", String.class, id))
                    .isEqualTo(SsoCrypto.hash(secret.getClientSecret()));
        } finally {
            UserContextHolder.clear();
        }
    }
}
