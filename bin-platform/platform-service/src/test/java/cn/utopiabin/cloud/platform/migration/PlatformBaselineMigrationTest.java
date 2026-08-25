package cn.utopiabin.cloud.platform.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformBaselineMigrationTest {

    @Test
    void baselineContainsOperateLogTableAndReadPermission() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V1__platform_iam_baseline.sql")) {
            assertTrue(input != null, "baseline migration must exist");
            String migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS sys_operate_log"));
            assertTrue(migration.contains("platform:operate-log:read"));
        }
    }

    @Test
    void phoneAuthMigrationContainsUniquePhoneAndSmsLog() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V2__phone_auth_and_sms.sql")) {
            assertTrue(input != null, "phone auth migration must exist");
            String migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(migration.contains("uk_user_tenant_phone"));
            assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS sys_sms_send_log"));
            assertTrue(migration.contains("idx_sms_log_tenant_phone_time"));
        }
    }
}
