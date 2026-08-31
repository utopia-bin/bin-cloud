package cn.utopiabin.cloud.platform.service;

import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import cn.utopiabin.cloud.platform.config.PlatformBootstrapConfiguration;
import cn.utopiabin.cloud.platform.config.PlatformBootstrapProperties;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class PlatformBootstrapServiceTest {

    private static final String TEST_PASSWORD = "BootstrapTest123!";
    private static final List<String> TABLES = List.of("sys_tenant", "sys_user", "sys_role",
            "sys_permission", "sys_user_role", "sys_role_permission", "sys_menu");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DatabaseConfiguration.class, PlatformBootstrapConfiguration.class,
                    PlatformBootstrapService.class);

    private ApplicationContextRunner enabledRunner() {
        return contextRunner.withPropertyValues("platform.bootstrap.enabled=true",
                "platform.bootstrap.password=" + TEST_PASSWORD);
    }

    @Test
    void shouldBeDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean("platformBootstrapRunner");
            context.getBean(PlatformBootstrapService.class).initialize();
            assertEmpty(context.getBean(JdbcTemplate.class));
        });
    }

    @Test
    void shouldCreateCompleteAdminFromRunnerWithEncodedPassword(CapturedOutput output) {
        enabledRunner().run(context -> {
            context.getBean("platformBootstrapRunner", ApplicationRunner.class).run(null);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            String hash = jdbc.queryForObject("SELECT password FROM sys_user WHERE username='admin'", String.class);
            assertThat(hash).isNotEqualTo(TEST_PASSWORD);
            assertThat(context.getBean(PasswordEncoder.class).matches(TEST_PASSWORD, hash)).isTrue();
            assertThat(output.getAll()).doesNotContain(TEST_PASSWORD, hash);
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_user u
                    JOIN sys_tenant t ON t.id=u.tenant_id
                    JOIN sys_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id
                    JOIN sys_role r ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id
                    JOIN sys_role_permission rp ON rp.role_id=r.id AND rp.tenant_id=r.tenant_id
                    JOIN sys_permission p ON p.id=rp.permission_id
                    WHERE t.code='default' AND u.username='admin' AND r.code='super_admin' AND p.code='*'
                    """, Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu", Integer.class)).isEqualTo(8);
        });
    }

    @Test
    void shouldNeverResetPasswordOrCreateAnotherAdminOnRestart() {
        enabledRunner().run(context -> {
            PlatformBootstrapService service = context.getBean(PlatformBootstrapService.class);
            service.initialize();
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            String hash = jdbc.queryForObject("SELECT password FROM sys_user", String.class);
            var properties = context.getBean(PlatformBootstrapProperties.class);
            properties.setPassword(null);
            properties.setUsername("another-admin");
            properties.setTenantCode("another-tenant");
            // 即使原角色被禁用或软删除，也不能利用重复初始化恢复超级管理员。
            jdbc.update("UPDATE sys_role SET available=0, is_delete=1");
            service.initialize();
            assertThat(jdbc.queryForObject("SELECT password FROM sys_user", String.class)).isEqualTo(hash);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_tenant", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT available FROM sys_role", Integer.class)).isZero();
        });
    }

    @Test
    void shouldRefuseToPromoteAnExistingAccount() {
        enabledRunner().run(context -> {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO sys_tenant (id,name,code) VALUES (1,'Existing','default')");
            jdbc.update("INSERT INTO sys_user (id,tenant_id,username,password,real_name) VALUES (2,1,'admin','existing-hash','Existing')");
            assertThatThrownBy(() -> context.getBean(PlatformBootstrapService.class).initialize())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("同名账号");
            assertThat(jdbc.queryForObject("SELECT password FROM sys_user", String.class)).isEqualTo("existing-hash");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class)).isZero();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "onlylowercaseletters", "123456789012"})
    void shouldRejectInvalidPasswordWithoutPersistingAnything(String password) {
        enabledRunner().withPropertyValues("platform.bootstrap.password=" + password).run(context -> {
            assertThatThrownBy(() -> context.getBean(PlatformBootstrapService.class).initialize())
                    .isInstanceOf(RuntimeException.class);
            assertEmpty(context.getBean(JdbcTemplate.class));
        });
    }

    @Test
    void shouldRejectPasswordOverBcryptByteLimit() {
        enabledRunner().run(context -> {
            context.getBean(PlatformBootstrapProperties.class).setPassword("Aa1" + "中".repeat(24));
            assertThatThrownBy(() -> context.getBean(PlatformBootstrapService.class).initialize())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("72");
            assertEmpty(context.getBean(JdbcTemplate.class));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"available=0", "is_delete=1", "tenant_id=7"})
    void shouldRefuseInvalidExistingWildcardPermission(String assignment) {
        enabledRunner().run(context -> {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO sys_permission (id,name,code,description) VALUES (1,'Existing','*','Existing')");
            jdbc.update("UPDATE sys_permission SET " + assignment);
            assertThatThrownBy(() -> context.getBean(PlatformBootstrapService.class).initialize())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("全局权限");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT description FROM sys_permission", String.class)).isEqualTo("Existing");
        });
    }

    @Test
    void shouldReuseSeededWildcardPermissionWithoutChangingIt() {
        enabledRunner().run(context -> {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO sys_permission (id,name,code,description) VALUES (1,'Existing','*','Existing')");
            context.getBean(PlatformBootstrapService.class).initialize();
            assertThat(jdbc.queryForObject("SELECT permission_id FROM sys_role_permission", Long.class)).isEqualTo(1L);
            assertThat(jdbc.queryForObject("SELECT description FROM sys_permission", String.class)).isEqualTo("Existing");
        });
    }

    @Test
    void shouldRejectInvalidIdentifierWithoutPersistingAnything() {
        enabledRunner().withPropertyValues("platform.bootstrap.username=invalid user").run(context -> {
            assertThatThrownBy(() -> context.getBean(PlatformBootstrapService.class).initialize())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("管理员用户名");
            assertEmpty(context.getBean(JdbcTemplate.class));
        });
    }

    @Test
    void shouldReuseAvailableTenantAndPreserveExistingMenu() {
        enabledRunner().run(context -> {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO sys_tenant (id,name,code) VALUES (1,'Existing','default')");
            jdbc.update("INSERT INTO sys_menu (id,type,name,path,icon,permission,sort) VALUES (1,2,'Custom','/tenant','Custom','custom:read',99)");
            context.getBean(PlatformBootstrapService.class).initialize();
            assertThat(jdbc.queryForObject("SELECT name FROM sys_tenant", String.class)).isEqualTo("Existing");
            assertThat(jdbc.queryForObject("SELECT tenant_id FROM sys_user", Long.class)).isEqualTo(1L);
            assertThat(jdbc.queryForObject("SELECT name FROM sys_menu WHERE path='/tenant'", String.class)).isEqualTo("Custom");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu", Integer.class)).isEqualTo(8);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"available=0", "is_delete=1", "expire_time='2000-01-01 00:00:00'"})
    void shouldRejectUnavailableTenant(String assignment) {
        enabledRunner().run(context -> {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.update("INSERT INTO sys_tenant (id,name,code) VALUES (1,'Existing','default')");
            jdbc.update("UPDATE sys_tenant SET " + assignment);
            assertThatThrownBy(() -> context.getBean(PlatformBootstrapService.class).initialize())
                    .isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isZero();
        });
    }

    @Test
    void shouldRollbackAllWritesAndAllowRetryAfterFailure() {
        enabledRunner().run(context -> {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.execute("ALTER TABLE sys_menu ADD CONSTRAINT reject_menu CHECK (sort < 0)");
            var service = context.getBean(PlatformBootstrapService.class);
            assertThatThrownBy(service::initialize).isInstanceOf(DataIntegrityViolationException.class);
            assertEmpty(jdbc);
            jdbc.execute("ALTER TABLE sys_menu DROP CONSTRAINT reject_menu");
            service.initialize();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isEqualTo(1);
        });
    }

    @Test
    void shouldSerializeConcurrentInitialization() {
        enabledRunner().run(context -> {
            var service = context.getBean(PlatformBootstrapService.class);
            var start = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> { start.await(); service.initialize(); return null; });
                var second = executor.submit(() -> { start.await(); service.initialize(); return null; });
                start.countDown();
                first.get(15, TimeUnit.SECONDS);
                second.get(15, TimeUnit.SECONDS);
            }
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class)).isEqualTo(1);
        });
    }

    private static void assertEmpty(JdbcTemplate jdbc) {
        for (String table : TABLES) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).as(table).isZero();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class DatabaseConfiguration {
        @Bean(destroyMethod = "shutdown")
        org.springframework.jdbc.datasource.embedded.EmbeddedDatabase dataSource() {
            var database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2).build();
            new JdbcTemplate(database).execute("SET MODE MySQL");
            new ResourceDatabasePopulator(new ClassPathResource("db/bootstrap-test-schema.sql")).execute(database);
            return database;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        PasswordValidator passwordValidator() {
            return new PasswordValidator(new LoginSecurityProperties());
        }
    }
}
