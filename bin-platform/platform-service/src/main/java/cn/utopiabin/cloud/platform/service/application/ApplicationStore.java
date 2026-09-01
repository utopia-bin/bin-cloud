package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;

import lombok.RequiredArgsConstructor;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用域持久层访问入口。
 *
 * <p>所有 SQL 均定义在 Mapper XML 中。本类只负责统一绑定参数、转换查询结果和处理分页结果， 防止 Service 层再次出现 SQL 字符串。
 */
@Repository
@RequiredArgsConstructor
public class ApplicationStore {
    private static final String NAMESPACE =
            "cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper.";

    private final SqlSessionTemplate sqlSession;

    public int update(String statement, Object... args) {
        return sqlSession.update(statement(statement), parameters(args));
    }

    public Map<String, Object> one(String statement, Object... args) {
        List<Map<String, Object>> rows = queryForList(statement, args);
        if (rows.size() != 1) {
            throw new BizException(404, "记录不存在或不属于当前授权范围");
        }
        return rows.getFirst();
    }

    public List<Map<String, Object>> queryForList(String statement, Object... args) {
        return sqlSession.selectList(statement(statement), parameters(args));
    }

    public <T> List<T> queryForList(String statement, Class<T> type, Object... args) {
        List<?> rows = sqlSession.selectList(statement(statement), parameters(args));
        return rows.stream().map(row -> convert(row, type)).toList();
    }

    public <T> List<T> list(Class<T> type, String statement, Object... args) {
        return queryForList(statement, type, args);
    }

    public <T> T queryForObject(String statement, Class<T> type, Object... args) {
        Object value = sqlSession.selectOne(statement(statement), parameters(args));
        return value == null ? null : convert(value, type);
    }

    public <T> PageResult<T> page(
            Class<T> type,
            ApplicationQuery query,
            String countStatement,
            String listStatement,
            Object... args) {
        int page = Math.max(1, query.getPage());
        int size = Math.max(1, Math.min(100, query.getSize()));
        Map<String, Object> parameters = parameters(args);
        parameters.put("query", query);
        parameters.put("limit", size);
        parameters.put("offset", (long) (page - 1) * size);
        Number total = sqlSession.selectOne(statement(countStatement), parameters);
        List<?> rows = sqlSession.selectList(statement(listStatement), parameters);
        List<T> records = rows.stream().map(row -> convert(row, type)).toList();
        return PageResult.of(page, size, total == null ? 0 : total.longValue(), records);
    }

    public static long number(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? 0 : ((Number) value).longValue();
    }

    public static boolean flag(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return Boolean.TRUE.equals(value) || value instanceof Number n && n.intValue() == 1;
    }

    public static LocalDateTime time(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value instanceof java.sql.Timestamp t ? t.toLocalDateTime() : (LocalDateTime) value;
    }

    public static void window(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start))
            throw new BizException(400, "到期时间必须晚于生效时间");
    }

    public static boolean within(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return (start == null || !start.isAfter(now)) && (end == null || end.isAfter(now));
    }

    public static void changed(int rows) {
        if (rows != 1) throw new BizException(409, "数据已变化，请刷新后重试");
    }

    public static int version(Integer version) {
        if (version == null || version < 0) {
            throw new BizException(400, "编辑或删除必须提供当前版本号");
        }
        return version;
    }

    private String statement(String statement) {
        return statement.indexOf('.') < 0 ? NAMESPACE + statement : statement;
    }

    private Map<String, Object> parameters(Object... args) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            parameters.put("p" + index, args[index]);
        }
        return parameters;
    }

    private <T> T convert(Object source, Class<T> type) {
        if (type.isInstance(source)) {
            return type.cast(source);
        }
        if (source instanceof Map<?, ?> values && isSimple(type)) {
            Object value = values.values().stream().findFirst().orElse(null);
            return value == null ? null : JsonUtil.convert(value, type);
        }
        if (source instanceof Map<?, ?> values) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            values.forEach((key, value) -> normalized.put(toCamelCase(String.valueOf(key)), value));
            return JsonUtil.convert(normalized, type);
        }
        return JsonUtil.convert(source, type);
    }

    private boolean isSimple(Class<?> type) {
        return type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type.isPrimitive();
    }

    private String toCamelCase(String name) {
        StringBuilder result = new StringBuilder(name.length());
        boolean upper = false;
        for (char character : name.toLowerCase().toCharArray()) {
            if (character == '_') {
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(character) : character);
                upper = false;
            }
        }
        return result.toString();
    }
}
