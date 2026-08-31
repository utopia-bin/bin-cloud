package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.platform.service.PermissionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.mockito.Mockito.*;

/** Actual V4 tables with MySQL storage/index syntax adapted; not a MySQL migration certification. */
class ApplicationFixture {
    final JdbcTemplate jdbc;
    final ApplicationStore store;
    final PermissionService permissions = mock(PermissionService.class);
    final ApplicationBoundary boundary;
    final TransactionTemplate transaction;
    final ApplicationRevocationService revocations;
    final SsoAuditService audit;
    ApplicationFixture() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE sys_tenant(id BIGINT PRIMARY KEY,name VARCHAR(100),code VARCHAR(50),available INT DEFAULT 1,is_delete INT DEFAULT 0,expire_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE sys_user(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(50),available INT DEFAULT 1,is_delete INT DEFAULT 0,credential_version INT DEFAULT 0,last_login_at TIMESTAMP,UNIQUE(tenant_id,id))");
        try (var in = getClass().getResourceAsStream("/db/migration/V4__application_catalog_and_sessions.sql")) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^\\s*KEY [^\\n]+\\n", "")
                    .replaceAll(",\\s*\\)", ")").replace(" STORED", "")
                    .replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci", "");
            for (String statement : sql.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
        }
        jdbc.execute("CREATE TABLE sys_permission(id BIGINT PRIMARY KEY,application_id BIGINT NOT NULL,name VARCHAR(100),code VARCHAR(100),description VARCHAR(500) DEFAULT '',sort INT DEFAULT 10,available INT DEFAULT 1,is_delete INT DEFAULT 0,version INT DEFAULT 0,UNIQUE(application_id,id),UNIQUE(application_id,code))");
        jdbc.execute("CREATE TABLE sys_role(id BIGINT PRIMARY KEY,tenant_id BIGINT NOT NULL,application_id BIGINT NOT NULL,tenant_application_id BIGINT NOT NULL,name VARCHAR(100),code VARCHAR(100),data_scope INT,available INT DEFAULT 1,is_delete INT DEFAULT 0,sort INT DEFAULT 10,version INT DEFAULT 0,UNIQUE(tenant_application_id,code),UNIQUE(tenant_id,application_id,tenant_application_id,id),FOREIGN KEY(tenant_id,application_id,tenant_application_id) REFERENCES sys_tenant_application(tenant_id,application_id,id))");
        jdbc.execute("CREATE TABLE sys_user_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,application_id BIGINT,tenant_application_id BIGINT,user_id BIGINT,role_id BIGINT,UNIQUE(tenant_application_id,user_id,role_id),FOREIGN KEY(tenant_id,application_id,tenant_application_id,role_id) REFERENCES sys_role(tenant_id,application_id,tenant_application_id,id),FOREIGN KEY(tenant_id,user_id) REFERENCES sys_user(tenant_id,id))");
        jdbc.execute("CREATE TABLE sys_role_permission(id BIGINT PRIMARY KEY,tenant_id BIGINT,application_id BIGINT,tenant_application_id BIGINT,role_id BIGINT,permission_id BIGINT,UNIQUE(tenant_application_id,role_id,permission_id),FOREIGN KEY(tenant_id,application_id,tenant_application_id,role_id) REFERENCES sys_role(tenant_id,application_id,tenant_application_id,id),FOREIGN KEY(application_id,permission_id) REFERENCES sys_permission(application_id,id))");
        jdbc.execute("CREATE TABLE sys_menu(id BIGINT PRIMARY KEY,application_id BIGINT,parent_id BIGINT DEFAULT 0,type INT DEFAULT 2,name VARCHAR(100),path VARCHAR(200) DEFAULT '',component VARCHAR(200) DEFAULT '',icon VARCHAR(100) DEFAULT '',permission VARCHAR(100) DEFAULT '',route_name VARCHAR(100) DEFAULT '',open_mode VARCHAR(16) DEFAULT 'INTERNAL',sort INT DEFAULT 10,visible INT DEFAULT 1,available INT DEFAULT 1,is_delete INT DEFAULT 0,version INT DEFAULT 0)");
        jdbc.update("INSERT INTO sys_tenant(id,name,code) VALUES(10,'Tenant A','a'),(20,'Tenant B','b')");
        jdbc.update("INSERT INTO sys_user(id,tenant_id,username) VALUES(100,10,'alice'),(101,10,'bob'),(200,20,'charlie')");
        TenantApplicationService.ensureConsole(jdbc,10);
        TenantApplicationService.ensureConsole(jdbc,20);
        jdbc.update("INSERT INTO sys_permission(id,application_id,name,code) VALUES(1,1,'Console','*'),(901,2,'Read','workbench:read'),(902,2,'Execute','workbench:execute')");
        jdbc.update("INSERT INTO sys_menu(id,application_id,name,path,permission) VALUES(901,2,'Workbench','/applications/workbench','workbench:read')");
        UserContextHolder.set(UserContext.of("100","alice","10",""));
        when(permissions.hasPermission(anyLong(),anyString())).thenReturn(true);
        store = new ApplicationStore(jdbc);
        boundary = new ApplicationBoundary(store,permissions);
        var proxy = new org.springframework.transaction.interceptor.TransactionProxyFactoryBean();
        proxy.setTarget(new SsoAuditService(jdbc));
        proxy.setTransactionManager(new DataSourceTransactionManager(ds));
        var attributes = new java.util.Properties(); attributes.setProperty("*", "PROPAGATION_REQUIRES_NEW");
        proxy.setTransactionAttributes(attributes); proxy.setProxyTargetClass(true); proxy.afterPropertiesSet();
        audit = (SsoAuditService) proxy.getObject();
        revocations = new ApplicationRevocationService(jdbc,audit);
    }
    long provision(long tenant,long user) {
        var dto = new cn.utopiabin.cloud.platform.model.dto.application.InstanceDTO();
        dto.setTenantId(tenant);dto.setApplicationId(2L);dto.setAdminUserId(user);
        return transaction.execute(s -> new TenantApplicationService(store,boundary,revocations).save(dto));
    }
}
