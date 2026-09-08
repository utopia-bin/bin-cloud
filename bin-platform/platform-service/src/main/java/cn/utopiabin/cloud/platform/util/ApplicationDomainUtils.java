package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.common.exception.BizException;
import java.time.LocalDateTime;

/** 应用域规则校验工具。 */
public final class ApplicationDomainUtils {

  private ApplicationDomainUtils() {}

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
