package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.common.context.UserContext;
import cn.utopiabin.cloud.common.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlatformTenantLineHandlerTest {

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void relationTablesAreTenantScoped() {
        var handler = new PlatformTenantLineHandler(new TenantProperties());

        assertFalse(handler.ignoreTable("sys_user_role"));
        assertFalse(handler.ignoreTable("sys_role_permission"));
    }

    @Test
    void jwtRoleCannotBypassTenantIsolation() {
        UserContextHolder.set(new UserContext("1", "admin", "9", List.of("SUPER_ADMIN")));
        var handler = new PlatformTenantLineHandler(new TenantProperties());

        assertFalse(handler.ignoreTable("sys_user"));
        assertEquals("9", handler.getTenantId().toString());
    }
}
