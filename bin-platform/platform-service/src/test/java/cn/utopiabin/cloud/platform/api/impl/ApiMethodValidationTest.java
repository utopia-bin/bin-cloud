package cn.utopiabin.cloud.platform.api.impl;

import cn.utopiabin.cloud.platform.api.auth.AuthApi;
import cn.utopiabin.cloud.platform.api.iam.SysPermissionApi;
import cn.utopiabin.cloud.platform.api.impl.auth.AuthApiImpl;
import cn.utopiabin.cloud.platform.api.impl.iam.SysPermissionApiImpl;
import cn.utopiabin.cloud.platform.api.impl.sms.SmsApiImpl;
import cn.utopiabin.cloud.platform.api.sms.SmsApi;
import cn.utopiabin.cloud.platform.model.dto.auth.LoginDTO;
import cn.utopiabin.cloud.platform.model.dto.sms.SmsCodeSendDTO;
import cn.utopiabin.cloud.platform.model.vo.auth.LoginResultVO;
import cn.utopiabin.cloud.platform.service.AuthService;
import cn.utopiabin.cloud.platform.service.SmsService;
import cn.utopiabin.cloud.platform.service.iam.SysPermissionService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import org.apache.dubbo.config.annotation.DubboService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApiMethodValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    static Stream<Class<?>> apiImplementations() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DubboService.class));
        var candidates = scanner.findCandidateComponents("cn.utopiabin.cloud.platform.api.impl");
        assertThat(candidates).hasSizeGreaterThanOrEqualTo(10);
        return candidates.stream().map(candidate -> {
            try {
                return Class.forName(candidate.getBeanClassName());
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    @ParameterizedTest(name = "{0} has valid inherited constraints")
    @MethodSource("apiImplementations")
    void shouldBuildValidationMetadataForEveryDubboProvider(Class<?> implementation) {
        assertThatCode(() -> validator.getConstraintsForClass(implementation))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPreserveAllExistingCascadedParameterChecks() {
        long cascadedParameters = apiImplementations()
                .flatMap(type -> Stream.of(type.getDeclaredMethods())
                        .map(method -> validator.getConstraintsForClass(type)
                                .getConstraintsForMethod(method.getName(), method.getParameterTypes())))
                .filter(descriptor -> descriptor != null)
                .flatMap(descriptor -> descriptor.getParameterDescriptors().stream())
                .filter(descriptor -> descriptor.isCascaded())
                .count();
        assertThat(cascadedParameters).isGreaterThanOrEqualTo(24);
    }

    @Test
    void shouldDelegateValidLoginThroughSpringValidationProxy() {
        AuthService service = mock(AuthService.class);
        LoginDTO request = new LoginDTO();
        request.setTenantCode("default");
        request.setUsername("admin");
        request.setPassword("test-only-password");
        LoginResultVO expected = new LoginResultVO();
        when(service.login(request)).thenReturn(expected);

        try (var context = authContext(service)) {
            assertThat(context.getBean(AuthApi.class).login(request)).isSameAs(expected);
            verify(service).login(request);
        }
    }

    @Test
    void shouldRejectInvalidLoginBeforeCallingBusinessService() {
        AuthService service = mock(AuthService.class);
        try (var context = authContext(service)) {
            assertThatThrownBy(() -> context.getBean(AuthApi.class).login(new LoginDTO()))
                    .isInstanceOfSatisfying(ConstraintViolationException.class, ex ->
                            assertThat(ex.getConstraintViolations())
                                    .extracting(violation -> violation.getPropertyPath().toString())
                                    .containsExactlyInAnyOrder("login.dto.tenantCode",
                                            "login.dto.username", "login.dto.password"));
            verifyNoInteractions(service);
        }
    }

    @Test
    void shouldRetainSmsDtoValidationOnInterface() throws Exception {
        var provider = new SmsApiImpl(mock(SmsService.class));
        var method = SmsApi.class.getMethod("sendVerificationCode", SmsCodeSendDTO.class);
        var violations = validator.forExecutables()
                .validateParameters(provider, method, new Object[]{new SmsCodeSendDTO()});
        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("sendVerificationCode.dto.tenantCode",
                        "sendVerificationCode.dto.phone", "sendVerificationCode.dto.scene");
    }

    @Test
    void shouldRetainPermissionIdNotNullChecksOnInterface() throws Exception {
        var provider = new SysPermissionApiImpl(mock(SysPermissionService.class));
        for (String methodName : new String[]{"get", "remove"}) {
            var method = SysPermissionApi.class.getMethod(methodName, Long.class);
            var violations = validator.forExecutables()
                    .validateParameters(provider, method, new Object[]{null});
            assertThat(violations).singleElement().satisfies(violation ->
                    assertThat(violation.getConstraintDescriptor().getAnnotation())
                            .isInstanceOf(NotNull.class));
        }
    }

    private AnnotationConfigApplicationContext authContext(AuthService service) {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(MethodValidationPostProcessor.class, () -> {
            var processor = new MethodValidationPostProcessor();
            processor.setValidator(validator);
            processor.setProxyTargetClass(true);
            return processor;
        });
        context.registerBean(AuthService.class, () -> service);
        context.registerBean(AuthApiImpl.class);
        context.refresh();
        return context;
    }
}
