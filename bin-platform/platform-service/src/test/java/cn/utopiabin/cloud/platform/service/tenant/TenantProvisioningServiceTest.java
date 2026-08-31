package cn.utopiabin.cloud.platform.service.tenant;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.config.LoginSecurityProperties;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantAdminDTO;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantProvisioningServiceTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private TenantProvisioningService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setup() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        jdbc.execute("CREATE TABLE sys_tenant(id BIGINT PRIMARY KEY, is_delete INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE sys_user(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES sys_tenant(id), username VARCHAR(50), password VARCHAR(100), real_name VARCHAR(50), is_delete INT DEFAULT 0, UNIQUE(tenant_id, username))");
        jdbc.execute("CREATE TABLE sys_role(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES sys_tenant(id), name VARCHAR(50), code VARCHAR(50), data_scope INT, UNIQUE(tenant_id, code))");
        jdbc.execute("CREATE TABLE sys_permission(id BIGINT PRIMARY KEY, tenant_id BIGINT, code VARCHAR(100), available INT DEFAULT 1, is_delete INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE sys_user_role(id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT REFERENCES sys_user(id), role_id BIGINT REFERENCES sys_role(id))");
        jdbc.execute("CREATE TABLE sys_role_permission(id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT REFERENCES sys_role(id), permission_id BIGINT REFERENCES sys_permission(id))");
        jdbc.execute("CREATE TABLE sys_tenant_application(id BIGINT PRIMARY KEY, tenant_id BIGINT, application_id BIGINT, status VARCHAR(16), access_policy VARCHAR(16), opened_at TIMESTAMP, is_delete INT DEFAULT 0)");
        for (String table : java.util.List.of("sys_role", "sys_permission", "sys_user_role", "sys_role_permission")) jdbc.execute("ALTER TABLE " + table + " ADD application_id BIGINT DEFAULT 1");
        for (String table : java.util.List.of("sys_role", "sys_user_role", "sys_role_permission")) jdbc.execute("ALTER TABLE " + table + " ADD tenant_application_id BIGINT");
        jdbc.update("INSERT INTO sys_tenant(id) VALUES(1),(2)");
        int id = 100;
        for (String code : TenantProvisioningService.ADMIN_PERMISSIONS) {
            jdbc.update("INSERT INTO sys_permission(id, code) VALUES(?, ?)", id++, code);
        }
        jdbc.update("INSERT INTO sys_permission(id, code) VALUES(1, '*'),(2, 'platform:tenant:create'),(3, 'platform:menu:update')");
        service = new TenantProvisioningService(jdbc, encoder, new PasswordValidator(new LoginSecurityProperties()));
    }

    private TenantAdminDTO credentials() {
        var dto = new TenantAdminDTO();
        dto.setAdminUsername("tenant_admin");
        dto.setAdminPassword("Test-Password123!");
        return dto;
    }

    @Test
    void provisionsUsableCredentialsAndOnlyTenantPermissionsWithoutOverwriting() {
        transaction.executeWithoutResult(status -> service.provision(1L, credentials()));
        String hash = jdbc.queryForObject("SELECT password FROM sys_user WHERE tenant_id=1", String.class);
        assertThat(encoder.matches(credentials().getAdminPassword(), hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id IN (1,2,3)", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission WHERE tenant_id=1", Integer.class)).isEqualTo(TenantProvisioningService.ADMIN_PERMISSIONS.size());
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> service.provision(1L, credentials())))
                .isInstanceOf(BizException.class).hasMessageContaining("不能重复开通");
        assertThat(jdbc.queryForObject("SELECT password FROM sys_user WHERE tenant_id=1", String.class)).isEqualTo(hash);
        transaction.executeWithoutResult(status -> service.provision(2L, credentials()));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isEqualTo(2);
    }

    @Test
    void rollsBackAllProvisionedRecordsWhenOuterTenantTransactionFails() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.provision(1L, credentials());
            throw new IllegalStateException("simulated commit failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission", Integer.class)).isZero();
    }

    @Test
    void rejectsIncompletePermissionsWithoutCreatingAnUnusableAccount() {
        jdbc.update("UPDATE sys_permission SET available=0 WHERE code='platform:user:read'");
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> service.provision(1L, credentials())))
                .isInstanceOf(BizException.class).hasMessageContaining("基础权限缺失");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isZero();
    }
}
