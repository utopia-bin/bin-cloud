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
}
