package cn.utopiabin.cloud.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 序列化 / 反序列化工具类
 *
 * @since 1.0.0
 */
@Slf4j
public final class JsonUtil {

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper()
                // 注册 Java 8 时间模块 (LocalDateTime / LocalDate / LocalTime 等)
                .registerModule(new JavaTimeModule())
                // 反序列化时忽略 JSON 中存在但 Java 对象不存在的属性
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 序列化时间时不转为 timestamp
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtil() {
    }

    /**
     * 获取全局共享的 ObjectMapper 实例
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 将对象序列化为 JSON 字符串
     */
    public static String toJson(Object source) {
        if (source == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为目标类型对象
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败, 目标类型 {}: {}", clazz.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为带泛型的目标类型对象
     */
    public static <T> T toObject(String json, TypeReference<T> typeRef) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("JSON 泛型反序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将对象转换为目标类型 (内部用 JSON 中转,适用于属性名一致的 DTO 转换)
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return MAPPER.convertValue(source, targetClass);
    }

    /**
     * 将对象转换为 Map (仅一层,不递归处理嵌套对象)
     */
    @SuppressWarnings("unchecked")
    public static <T> java.util.Map<String, T> toMap(Object source) {
        if (source == null) {
            return null;
        }
        return MAPPER.convertValue(source, java.util.Map.class);
    }

    /**
     * 将 JSON 字符串解析为 JsonNode 树
     */
    public static JsonNode readTree(String json) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析为 JsonNode 失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
