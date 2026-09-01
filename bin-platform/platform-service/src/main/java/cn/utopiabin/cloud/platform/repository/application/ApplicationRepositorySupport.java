package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用域 Repository 公共能力。
 *
 * <p>只处理 MyBatis 查询参数、结果映射和分页组装，不承载业务规则。
 */
abstract class ApplicationRepositorySupport {

    protected Map<String, Object> parameters(Object... args) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            parameters.put("p" + index, args[index]);
        }
        return parameters;
    }

    protected Map<String, Object> one(List<Object> rows) {
        if (rows.size() != 1 || !(rows.getFirst() instanceof Map<?, ?> row)) {
            throw new BizException(404, "记录不存在或不属于当前授权范围");
        }
        return castMap(row);
    }

    protected List<Map<String, Object>> maps(List<Object> rows) {
        return rows.stream()
                .map(
                        row -> {
                            if (!(row instanceof Map<?, ?> values)) {
                                throw new IllegalStateException("Mapper 查询结果不是键值结构");
                            }
                            return castMap(values);
                        })
                .toList();
    }

    protected <T> List<T> convert(List<Object> rows, Class<T> type) {
        return rows.stream().map(row -> convert(row, type)).toList();
    }

    protected <T> PageResult<T> page(
            ApplicationQuery query, Long total, List<Object> rows, Class<T> type) {
        int page = Math.max(1, query.getPage());
        int size = Math.max(1, Math.min(100, query.getSize()));
        return PageResult.of(page, size, total == null ? 0 : total, convert(rows, type));
    }

    protected Map<String, Object> pageParameters(ApplicationQuery query, Object... args) {
        int page = Math.max(1, query.getPage());
        int size = Math.max(1, Math.min(100, query.getSize()));
        Map<String, Object> parameters = parameters(args);
        parameters.put("query", query);
        parameters.put("limit", size);
        parameters.put("offset", (long) (page - 1) * size);
        return parameters;
    }

    protected Long scalarLong(List<Object> rows) {
        if (rows.isEmpty() || rows.getFirst() == null) {
            return null;
        }
        Object value = unwrap(rows.getFirst());
        return ((Number) value).longValue();
    }

    protected <T> List<T> scalars(List<Object> rows, Class<T> type) {
        return rows.stream().map(this::unwrap).map(value -> JsonUtil.convert(value, type)).toList();
    }

    private Object unwrap(Object value) {
        if (value instanceof Map<?, ?> map && map.size() == 1) {
            return map.values().iterator().next();
        }
        return value;
    }

    private <T> T convert(Object source, Class<T> type) {
        if (type.isInstance(source)) {
            return type.cast(source);
        }
        if (source instanceof Map<?, ?> values) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            values.forEach((key, value) -> normalized.put(toCamelCase(String.valueOf(key)), value));
            return JsonUtil.convert(normalized, type);
        }
        return JsonUtil.convert(source, type);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
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
