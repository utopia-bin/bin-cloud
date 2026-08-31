package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JDBC queries in this domain always bind explicit tenant/application boundaries. */
@Repository
@RequiredArgsConstructor
public class ApplicationStore {
    private final JdbcTemplate jdbc;

    public JdbcTemplate jdbc() { return jdbc; }

    public Map<String, Object> one(String sql, Object... args) {
        var rows = jdbc.queryForList(sql, args);
        if (rows.size() != 1) throw new BizException(404, "记录不存在或不属于当前授权范围");
        return rows.getFirst();
    }

    public <T> List<T> list(Class<T> type, String sql, Object... args) {
        return jdbc.query(sql, BeanPropertyRowMapper.newInstance(type), args);
    }

    public <T> PageResult<T> page(Class<T> type, ApplicationQuery query, String from, String select, String order, List<Object> args) {
        int page = Math.max(1, query.getPage());
        int size = Math.max(1, Math.min(100, query.getSize()));
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + from, Long.class, args.toArray());
        var paged = new ArrayList<>(args);
        paged.add(size);
        paged.add((long) (page - 1) * size);
        return PageResult.of(page, size, total == null ? 0 : total,
                list(type, "SELECT " + select + " " + from + " ORDER BY " + order + " LIMIT ? OFFSET ?", paged.toArray()));
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
        if (start != null && end != null && !end.isAfter(start)) throw new BizException(400, "到期时间必须晚于生效时间");
    }

    public static boolean within(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return (start == null || !start.isAfter(now)) && (end == null || end.isAfter(now));
    }

    public static void changed(int rows) {
        if (rows != 1) throw new BizException(409, "数据已变化，请刷新后重试");
    }

    public static int version(Integer version) {
        if (version == null || version < 0) throw new BizException(400, "编辑或删除必须提供当前版本号");
        return version;
    }
}
