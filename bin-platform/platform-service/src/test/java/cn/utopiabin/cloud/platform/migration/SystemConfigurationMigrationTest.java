package cn.utopiabin.cloud.platform.migration;

import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.api.impl.system.SysDictApiImpl;
import cn.utopiabin.cloud.platform.api.impl.system.SysParameterApiImpl;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemConfigurationMigrationTest {

    @Test
    void migrationsCoverEveryMappedEntityAndItsPersistentFields() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var tables = new HashMap<String, Set<String>>();
        var create = Pattern.compile("(?is)CREATE TABLE (?:IF NOT EXISTS\\s+)?\\s*(\\w+)\\s*\\((.*?)\\)\\s*ENGINE=");
        var column = Pattern.compile("(?m)^\\s+(\\w+)\\s+\\w+");
        for (var resource : resolver.getResources("classpath*:db/migration/V*__*.sql")) {
            var matcher = create.matcher(resource.getContentAsString(StandardCharsets.UTF_8));
            while (matcher.find()) {
                var columns = new HashSet<String>();
                var fields = column.matcher(matcher.group(2));
                while (fields.find()) columns.add(fields.group(1));
                tables.put(matcher.group(1), columns);
            }
        }
        var alter = Pattern.compile("(?is)ALTER TABLE\\s+(\\w+)\\s+([^;]+)");
        var added = Pattern.compile("(?i)ADD\\s+(?:COLUMN\\s+)?(\\w+)\\s+(?:BIGINT|INT|VARCHAR|DATETIME|TINYINT)\\b");
        for (var resource : resolver.getResources("classpath*:db/migration/V*__*.sql")) {
            var statements = alter.matcher(resource.getContentAsString(StandardCharsets.UTF_8));
            while (statements.find()) {
                var fields = added.matcher(statements.group(2));
                while (fields.find()) tables.computeIfAbsent(statements.group(1), key -> new HashSet<>()).add(fields.group(1));
            }
        }
        int checked = 0;
        var metadata = new SimpleMetadataReaderFactory();
        for (var resource : resolver.getResources("classpath*:cn/utopiabin/cloud/platform/entity/**/*.class")) {
            var entity = Class.forName(metadata.getMetadataReader(resource).getClassMetadata().getClassName());
            var table = entity.getAnnotation(TableName.class);
            if (table == null) continue;
            assertThat(tables).as("Missing migration for %s", entity.getName()).containsKey(table.value());
            var excluded = Arrays.asList(table.excludeProperty());
            for (Class<?> type = entity; type != Object.class; type = type.getSuperclass()) {
                for (var field : type.getDeclaredFields()) {
                    var mapping = field.getAnnotation(TableField.class);
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())
                            || excluded.contains(field.getName()) || mapping != null && !mapping.exist()) continue;
                    String name = mapping != null && !mapping.value().isBlank() ? mapping.value()
                            : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
                    assertThat(tables.get(table.value())).as("%s.%s", table.value(), name).contains(name);
                }
            }
            checked++;
        }
        assertThat(checked).isGreaterThanOrEqualTo(12);
    }

    @Test
    void everySystemConfigurationApiHasAnExistingPermissionCode() throws Exception {
        String sql = migration();
        for (var api : new Class<?>[]{SysDictApiImpl.class, SysParameterApiImpl.class}) {
            for (var method : api.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                var required = method.getAnnotation(RequirePermission.class);
                assertThat(required).as("%s.%s", api.getSimpleName(), method.getName()).isNotNull();
                assertThat(sql).contains("'" + required.value() + "'");
            }
        }
    }

    @Test
    void newTablesSupportTenantIsolationSoftDeleteAndNonDestructiveReruns() throws Exception {
        var database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                .setName(UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE").build();
        try {
            var jdbc = new JdbcTemplate(database);
            jdbc.execute("CREATE TABLE sys_tenant (id BIGINT PRIMARY KEY)");
            jdbc.execute("""
                    CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(50),
                    code VARCHAR(100) UNIQUE, description VARCHAR(200), available TINYINT, sort INT,
                    version INT, is_delete TINYINT)
                    """);
            jdbc.update("INSERT INTO sys_tenant VALUES (1), (2)");
            // H2 checks the DDL/data constraints; MySQL-only storage/collation directives are omitted.
            String sql = migration().replaceAll("(?i)\\s+STORED", "")
                    .replaceAll("(?i)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci", "");
            try (var connection = database.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            }
            jdbc.update("INSERT INTO sys_dict (id, tenant_id, name, code) VALUES (10,1,'状态','status'),(20,2,'状态','status')");
            assertThatThrownBy(() -> jdbc.update("INSERT INTO sys_dict (id,tenant_id,name,code) VALUES (11,1,'新名称','status')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            jdbc.update("INSERT INTO sys_dict_options (id,tenant_id,dict_id,option_name,option_value) VALUES (100,1,10,'启用','1')");
            assertThatThrownBy(() -> jdbc.update("INSERT INTO sys_dict_options (id,tenant_id,dict_id,option_name,option_value) VALUES (101,2,10,'启用','1')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            jdbc.update("INSERT INTO sys_parameter (id,tenant_id,param_key,param_value) VALUES (1,1,'theme','dark'),(2,2,'theme','light')");
            assertThatThrownBy(() -> jdbc.update("INSERT INTO sys_parameter (id,tenant_id,param_key,param_value) VALUES (3,1,'theme','other')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            jdbc.update("UPDATE sys_parameter SET is_delete=1 WHERE id=1");
            jdbc.update("INSERT INTO sys_parameter (id,tenant_id,param_key,param_value) VALUES (3,1,'theme','new')");
            jdbc.update("UPDATE sys_permission SET available=0 WHERE code='platform:dict:read'");
            try (var connection = database.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            }
            assertThat(jdbc.queryForObject("SELECT available FROM sys_permission WHERE code='platform:dict:read'", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class)).isEqualTo(8);
            assertThat(jdbc.queryForObject("SELECT param_value FROM sys_parameter WHERE id=3", String.class)).isEqualTo("new");
        } finally {
            database.shutdown();
        }
    }

    private String migration() throws Exception {
        var resource = new org.springframework.core.io.ClassPathResource("db/migration/V3__system_dict_and_parameter.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
