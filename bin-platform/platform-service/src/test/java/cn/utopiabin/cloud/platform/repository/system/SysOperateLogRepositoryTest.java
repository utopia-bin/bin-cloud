package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SysOperateLogRepositoryTest {

    private final SysOperateLogRepository repository = new SysOperateLogRepository();

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void noTenantContextAllowsControlledPlatformQuery() {
        assertNull(repository.currentTenantId());
    }

    @Test
    void validTenantContextIsApplied() {
        UserContextHolder.set(new UserContext("1", "admin", "42", List.of()));

        assertEquals(42L, repository.currentTenantId());
    }

    @Test
    void invalidTenantContextFallsBackToNonexistentTenant() {
        UserContextHolder.set(new UserContext("1", "admin", "invalid", List.of()));

        assertEquals(0L, repository.currentTenantId());
    }
}
