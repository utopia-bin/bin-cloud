package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.common.exception.BizException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/** 应用域数据转换与规则校验工具。 */
public final class ApplicationDomainUtils {

    private ApplicationDomainUtils() {}

    public static long number(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? 0 : ((Number) value).longValue();
    }

    public static boolean flag(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return Boolean.TRUE.equals(value)
                || value instanceof Number number && number.intValue() == 1;
    }

    public static LocalDateTime time(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value instanceof Timestamp timestamp
                ? timestamp.toLocalDateTime()
                : (LocalDateTime) value;
    }

    public static void validateWindow(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BizException(400, "到期时间必须晚于生效时间");
        }
    }

    public static boolean isWithin(
            LocalDateTime start, LocalDateTime end, LocalDateTime currentTime) {
        return (start == null || !start.isAfter(currentTime))
                && (end == null || end.isAfter(currentTime));
    }

    public static void requireSingleChange(int affectedRows) {
        if (affectedRows != 1) {
            throw new BizException(409, "数据已变化，请刷新后重试");
        }
    }

    public static int requireVersion(Integer version) {
        if (version == null || version < 0) {
            throw new BizException(400, "编辑或删除必须提供当前版本号");
        }
        return version;
    }
}
