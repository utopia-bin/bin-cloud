package cn.utopiabin.cloud.platform.api.impl.system;

import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.system.SysOperateLogPageQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SysOperateLogApiImplTest {

    @Test
    void pageRequiresOperateLogReadPermission() throws NoSuchMethodException {
        var method = SysOperateLogApiImpl.class.getMethod("page", SysOperateLogPageQuery.class);
        var permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(permission);
        assertEquals("platform:operate-log:read", permission.value());
    }
}
