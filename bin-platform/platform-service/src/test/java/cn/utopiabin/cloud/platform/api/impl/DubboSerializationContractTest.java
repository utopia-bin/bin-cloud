package cn.utopiabin.cloud.platform.api.impl;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.hessian2.Hessian2Serialization;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityConfigurator;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the actual wire serializer: injvm calls alone do not serialize RPC payloads. */
class DubboSerializationContractTest {

    private static FrameworkModel framework;
    private static URL url;
    private static List<Class<?>> contracts;
    private static final Hessian2Serialization SERIALIZER = new Hessian2Serialization();

    @BeforeAll
    static void prepareStrictSerialization() throws Exception {
        contracts = new ArrayList<>();
        var resolver = new PathMatchingResourcePatternResolver();
        var metadata = new SimpleMetadataReaderFactory();
        for (var resource : resolver.getResources("classpath*:cn/utopiabin/cloud/platform/api/**/*.class")) {
            var type = Class.forName(metadata.getMetadataReader(resource).getClassMetadata().getClassName());
            if (type.isInterface()) {
                contracts.add(type);
            }
        }
        assertThat(contracts).hasSizeGreaterThanOrEqualTo(10);
        framework = new FrameworkModel();
        var module = framework.newApplication().newModule();
        var manager = framework.getBeanFactory().getBean(SerializeSecurityManager.class);
        manager.setCheckStatus(SerializeCheckStatus.STRICT);
        manager.setCheckSerializable(true);
        var configurator = module.getBeanFactory().getBean(SerializeSecurityConfigurator.class);
        // Use Dubbo's normal API-type discovery, not a broad package allowlist in the test.
        contracts.forEach(configurator::registerInterface);
        url = new URL("dubbo", "127.0.0.1", 20880).setScopeModel(module);
    }

    @AfterAll
    static void destroyFramework() {
        if (framework != null) {
            framework.destroy();
        }
    }

    static Stream<Class<?>> transportModels() {
        var visited = new HashSet<Type>();
        var models = new LinkedHashSet<Class<?>>();
        for (Class<?> contract : contracts) {
            for (var method : contract.getMethods()) {
                inspectType(method.getGenericReturnType(), visited, models);
                for (Type type : method.getGenericParameterTypes()) {
                    inspectType(type, visited, models);
                }
                for (Type type : method.getGenericExceptionTypes()) {
                    inspectType(type, visited, models);
                }
            }
        }
        assertThat(models).contains(PageResult.class);
        return models.stream().filter(type -> !type.isEnum() && !Modifier.isAbstract(type.getModifiers()));
    }

    private static void inspectType(Type type, Set<Type> visited, Set<Class<?>> models) {
        if (type == null || !visited.add(type)) {
            return;
        }
        if (type instanceof ParameterizedType parameterized) {
            inspectType(parameterized.getRawType(), visited, models);
            for (Type argument : parameterized.getActualTypeArguments()) {
                inspectType(argument, visited, models);
            }
        } else if (type instanceof GenericArrayType array) {
            inspectType(array.getGenericComponentType(), visited, models);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) {
                inspectType(bound, visited, models);
            }
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) {
                inspectType(bound, visited, models);
            }
            for (Type bound : wildcard.getLowerBounds()) {
                inspectType(bound, visited, models);
            }
        } else if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                inspectType(clazz.getComponentType(), visited, models);
            } else if (clazz.getName().startsWith("cn.utopiabin.cloud.")) {
                assertThat(Serializable.class.isAssignableFrom(clazz))
                        .as("RPC model %s must be Serializable", clazz.getName()).isTrue();
                models.add(clazz);
                inspectType(clazz.getGenericSuperclass(), visited, models);
                if (!clazz.isEnum()) {
                    for (var field : clazz.getDeclaredFields()) {
                        if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                            inspectType(field.getGenericType(), visited, models);
                        }
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "{0} round-trips under STRICT Hessian2")
    @MethodSource("transportModels")
    void shouldRoundTripEveryReachableModel(Class<?> type) throws Exception {
        assertRoundTrip(sample(type, 0));
    }

    @Test
    void shouldRoundTripNonEmptyResultsForEveryPagedApi() throws Exception {
        int tested = 0;
        for (Class<?> contract : contracts) {
            for (var method : contract.getMethods()) {
                if (method.getGenericReturnType() instanceof ParameterizedType returnType
                        && returnType.getRawType() == PageResult.class) {
                    var record = sample(returnType.getActualTypeArguments()[0], 0);
                    var page = PageResult.of(2, 10, 11, List.of(record));
                    assertThat(page).isInstanceOf(JsonSerializable.class);
                    assertRoundTrip(page);
                    tested++;
                }
            }
        }
        assertThat(tested).isGreaterThanOrEqualTo(7);
    }

    @Test
    void shouldRoundTripEmptyAndNullRecordPages() throws Exception {
        assertRoundTrip(PageResult.empty());
        assertRoundTrip(PageResult.empty(3, 20));
        assertRoundTrip(PageResult.of(1, 10, 0, null));
    }

    private static void assertRoundTrip(Object value) throws Exception {
        var bytes = new ByteArrayOutputStream();
        var output = SERIALIZER.serialize(url, bytes);
        output.writeObject(value);
        output.flushBuffer();
        var input = SERIALIZER.deserialize(url, new ByteArrayInputStream(bytes.toByteArray()));
        Object restored = input.readObject(value.getClass());
        assertThat(restored).isExactlyInstanceOf(value.getClass());
        assertThat(JsonUtil.toJson(restored)).isEqualTo(JsonUtil.toJson(value));
    }

    /** Fill inherited fields, enum/date values and nested lists, including bounded tree children. */
    private static Object sample(Type type, int depth) throws Exception {
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == List.class) {
            Object item = depth > 3 ? null : sample(parameterized.getActualTypeArguments()[0], depth + 1);
            return item == null ? List.of() : List.of(item);
        }
        if (type instanceof TypeVariable<?>) {
            return null;
        }
        if (!(type instanceof Class<?> clazz)) {
            throw new IllegalArgumentException("Add a sample generator for RPC field: " + type);
        }
        if (clazz == cn.utopiabin.cloud.common.exception.BizException.class) return new cn.utopiabin.cloud.common.exception.BizException(409, "资源已修改，请刷新后重试");
        if (clazz == String.class) return "test-value";
        if (clazz == long.class || clazz == Long.class) return 17L;
        if (clazz == int.class || clazz == Integer.class) return 3;
        if (clazz == boolean.class || clazz == Boolean.class) return true;
        if (clazz == Date.class) return new Date(1700000000000L);
        if (clazz == LocalDateTime.class) return LocalDateTime.of(2026, 8, 31, 12, 0);
        if (clazz.isEnum()) return clazz.getEnumConstants()[0];
        if (depth > 3) return null;
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(instance, sample(field.getGenericType(), depth + 1));
                }
            }
        }
        return instance;
    }
}
