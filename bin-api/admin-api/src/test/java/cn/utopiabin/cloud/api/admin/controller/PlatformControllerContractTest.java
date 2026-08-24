package cn.utopiabin.cloud.api.admin.controller;

import cn.utopiabin.cloud.api.admin.controller.auth.AuthController;
import cn.utopiabin.cloud.api.admin.controller.iam.SysMenuController;
import cn.utopiabin.cloud.api.admin.controller.iam.SysPermissionController;
import cn.utopiabin.cloud.api.admin.controller.iam.SysRoleController;
import cn.utopiabin.cloud.api.admin.controller.iam.SysUserController;
import cn.utopiabin.cloud.api.admin.controller.system.SysDictController;
import cn.utopiabin.cloud.api.admin.controller.system.SysOperateLogController;
import cn.utopiabin.cloud.api.admin.controller.system.SysParameterController;
import cn.utopiabin.cloud.api.admin.controller.tenant.TenantController;
import cn.utopiabin.cloud.platform.api.auth.AuthApi;
import cn.utopiabin.cloud.platform.api.iam.SysMenuApi;
import cn.utopiabin.cloud.platform.api.iam.SysPermissionApi;
import cn.utopiabin.cloud.platform.api.iam.SysRoleApi;
import cn.utopiabin.cloud.platform.api.iam.SysUserApi;
import cn.utopiabin.cloud.platform.api.system.SysDictApi;
import cn.utopiabin.cloud.platform.api.system.SysOperateLogApi;
import cn.utopiabin.cloud.platform.api.system.SysParameterApi;
import cn.utopiabin.cloud.platform.api.tenant.TenantApi;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformControllerContractTest {

    private static final Map<Class<?>, Class<?>> API_CONTROLLERS = Map.of(
            AuthApi.class, AuthController.class,
            TenantApi.class, TenantController.class,
            SysUserApi.class, SysUserController.class,
            SysRoleApi.class, SysRoleController.class,
            SysMenuApi.class, SysMenuController.class,
            SysPermissionApi.class, SysPermissionController.class,
            SysDictApi.class, SysDictController.class,
            SysParameterApi.class, SysParameterController.class,
            SysOperateLogApi.class, SysOperateLogController.class
    );

    @Test
    void everyPlatformApiMethodHasControllerEndpoint() {
        API_CONTROLLERS.forEach((apiType, controllerType) -> {
            Set<String> apiMethods = Arrays.stream(apiType.getMethods())
                    .map(method -> method.getName())
                    .collect(Collectors.toSet());
            Set<String> controllerMethods = Arrays.stream(controllerType.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .map(method -> method.getName())
                    .collect(Collectors.toSet());

            assertTrue(controllerMethods.containsAll(apiMethods),
                    () -> controllerType.getSimpleName() + " 未覆盖 " + difference(apiMethods, controllerMethods));
        });
    }

    @Test
    void everyControllerIsRestControllerWithUniqueBasePath() {
        Set<String> paths = API_CONTROLLERS.values().stream()
                .map(controllerType -> {
                    assertTrue(AnnotatedElementUtils.hasAnnotation(controllerType, RestController.class));
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                            controllerType, RequestMapping.class);
                    assertTrue(mapping != null && mapping.value().length == 1,
                            () -> controllerType.getSimpleName() + " 缺少唯一基础路径");
                    return mapping.value()[0];
                })
                .collect(Collectors.toSet());

        assertEquals(API_CONTROLLERS.size(), paths.size(), "Controller 基础路径不应重复");
    }

    private Set<String> difference(Set<String> expected, Set<String> actual) {
        return expected.stream()
                .filter(method -> !actual.contains(method))
                .collect(Collectors.toSet());
    }
}
