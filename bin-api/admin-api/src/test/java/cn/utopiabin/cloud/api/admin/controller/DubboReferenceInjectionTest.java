package cn.utopiabin.cloud.api.admin.controller;

import cn.utopiabin.cloud.api.admin.AdminApplication;
import cn.utopiabin.cloud.api.admin.controller.auth.AuthController;
import cn.utopiabin.cloud.platform.api.auth.AuthApi;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AdminApplication.class, properties = {
        "spring.config.import=",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "dubbo.application.name=admin-dubbo-injection-test",
        "dubbo.application.qos-enable=false",
        "dubbo.registry.address=N/A",
        "dubbo.consumer.check=false",
        "dubbo.consumer.scope=local",
        "dubbo.protocol.name=injvm",
        "dubbo.protocol.port=-1",
        "gateway.context.signing-secret=0123456789abcdef0123456789abcdef"
})
@Import(DubboReferenceInjectionTest.LocalProviderConfiguration.class)
@DirtiesContext
class DubboReferenceInjectionTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AuthController controller;

    @Test
    void shouldInjectEveryControllerReference() {
        int references = 0;
        for (Object bean : context.getBeansWithAnnotation(RestController.class).values()) {
            Object target = AopTestUtils.getUltimateTargetObject(bean);
            for (var field : target.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(DubboReference.class)) {
                    assertThat(ReflectionTestUtils.getField(target, field.getName()))
                            .as(target.getClass().getSimpleName() + "." + field.getName())
                            .isNotNull().isInstanceOf(field.getType());
                    references++;
                }
            }
        }
        assertThat(references).isGreaterThanOrEqualTo(10);
    }

    @Test
    void shouldLoadCoreDubboAutoConfiguration() {
        assertThat(context.getBeansOfType(DubboAutoConfiguration.class)).hasSize(1);
    }

    @Test
    void shouldCallLoginThroughAnInjectedDubboProxy() {
        LoginDTO request = new LoginDTO();
        request.setTenantCode("default");
        request.setUsername("admin");
        request.setPassword("TestOnly123!");

        assertThat(controller.login(request).getData().getToken()).isEqualTo("test-only-token");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalProviderConfiguration {
        @Bean
        ServiceBean<AuthApi> testAuthProvider(ApplicationContext applicationContext) {
            AuthApi implementation = mock(AuthApi.class);
            LoginResultVO result = new LoginResultVO();
            result.setToken("test-only-token");
            when(implementation.login(any(LoginDTO.class))).thenReturn(result);
            ServiceBean<AuthApi> service = new ServiceBean<>(applicationContext);
            service.setInterface(AuthApi.class);
            service.setRef(implementation);
            service.setScope("local");
            return service;
        }
    }
}
