package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.platform.mapper.iam.SysUserMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Executes generated updateById SQL with the production interceptor chain, without external services. */
class MyBatisPlusUpdateTest {
    private SqlSessionFactory sessions;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE sys_user (credential_version INT DEFAULT 0,
                    id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
                    username VARCHAR(50), password VARCHAR(100), real_name VARCHAR(50),
                    phone VARCHAR(20), email VARCHAR(100), gender INT, available BOOLEAN,
                    sort INT, comment VARCHAR(500), version INT NOT NULL DEFAULT 0,
                    is_delete INT NOT NULL DEFAULT 0, gmt_create TIMESTAMP, gmt_modify TIMESTAMP,
                    create_user VARCHAR(64), modify_user VARCHAR(64)
                )
                """);
        jdbc.update("INSERT INTO sys_user(id, tenant_id, username, available) VALUES (10, 1, 'fixture', TRUE)");

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(SysUserMapper.class);
        configuration.addMapper(cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper.class);
        jdbc.execute("CREATE TABLE sys_permission(id BIGINT PRIMARY KEY,application_id BIGINT,tenant_id BIGINT,name VARCHAR(50),code VARCHAR(100),description VARCHAR(200),available INT DEFAULT 1,sort INT DEFAULT 10,version INT DEFAULT 0,is_delete INT DEFAULT 0,gmt_create TIMESTAMP,gmt_modify TIMESTAMP,create_user VARCHAR(64),modify_user VARCHAR(64))");
        jdbc.update("INSERT INTO sys_permission(id,application_id,name,code) VALUES(1,1,'Console','read'),(2,2,'Other app','read')");
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(new GlobalConfig().setMetaObjectHandler(new AuditMetaObjectHandler()));
        factory.setPlugins(new MyBatisPlusConfig(new PlatformTenantLineHandler(new TenantProperties()))
                .mybatisPlusInterceptor());
        sessions = factory.getObject();
        useTenant("1");
    }

    @AfterEach
    void cleanup() {
        UserContextHolder.clear();
        jdbc.execute("SHUTDOWN");
    }

    private void useTenant(String tenantId) {
        var context = new UserContext();
        context.setTenantId(tenantId);
        context.setUserId("99");
        UserContextHolder.set(context);
    }

    @Test
    void consoleMapperNeverReadsOrMutatesAnotherApplicationEvenDuringLogin() {
        cn.utopiabin.cloud.platform.tenant.TenantIgnoreContext.enable();
        try (var session = sessions.openSession(true)) {
            var mapper = session.getMapper(cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper.class);
            assertThat(mapper.selectList(null)).extracting(cn.utopiabin.cloud.platform.entity.iam.SysPermission::getId).containsExactly(1L);
            assertThat(mapper.selectById(2L)).isNull();
            assertThat(mapper.deleteById(2L)).isZero();
            assertThat(jdbc.queryForObject("SELECT is_delete FROM sys_permission WHERE id=2",Integer.class)).isZero();
        } finally { cn.utopiabin.cloud.platform.tenant.TenantIgnoreContext.clear(); }
    }

    @Test
    void updateBindsOriginalVersionAndIncrementsStoredVersion() {
        try (var session = sessions.openSession(true)) {
            var mapper = session.getMapper(SysUserMapper.class);
            var user = mapper.selectById(10L);
            user.setRealName("Updated fixture");
            assertThat(mapper.updateById(user)).isEqualTo(1);
            var stored = mapper.selectById(10L);
            assertThat(stored.getVersion()).isEqualTo(1);
            assertThat(stored.getRealName()).isEqualTo("Updated fixture");
        }
    }

    @Test
    void staleVersionCannotOverwriteConcurrentUpdate() {
        try (var session = sessions.openSession(true)) {
            var mapper = session.getMapper(SysUserMapper.class);
            var stale = mapper.selectById(10L);
            jdbc.update("UPDATE sys_user SET version = 1, real_name = 'Concurrent edit' WHERE id = 10");
            stale.setRealName("Stale edit");
            assertThat(mapper.updateById(stale)).isZero();
            assertThat(mapper.selectById(10L).getRealName()).isEqualTo("Concurrent edit");
        }
    }

    @Test
    void optimisticLockPreservesTenantIsolation() {
        try (var session = sessions.openSession(true)) {
            var mapper = session.getMapper(SysUserMapper.class);
            var user = mapper.selectById(10L);
            useTenant("2");
            user.setRealName("Other tenant");
            assertThat(mapper.updateById(user)).isZero();
            assertThat(jdbc.queryForObject("SELECT version FROM sys_user WHERE id = 10", Integer.class)).isZero();
        }
    }

    @Test
    void optimisticLockDoesNotUpdateDeletedUsers() {
        try (var session = sessions.openSession(true)) {
            var mapper = session.getMapper(SysUserMapper.class);
            var user = mapper.selectById(10L);
            jdbc.update("UPDATE sys_user SET is_delete = 1 WHERE id = 10");
            user.setRealName("Deleted user");
            assertThat(mapper.updateById(user)).isZero();
            assertThat(jdbc.queryForObject("SELECT version FROM sys_user WHERE id = 10", Integer.class)).isZero();
        }
    }
}
