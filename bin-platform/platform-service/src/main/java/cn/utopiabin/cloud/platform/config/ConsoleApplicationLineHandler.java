package cn.utopiabin.cloud.platform.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import java.util.Locale;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/** 原有 IAM Mapper 固定访问平台壳；跨应用 Mapper 由领域 Repository 显式限定边界。 */
public class ConsoleApplicationLineHandler implements TenantLineHandler {
  private static final Set<String> TABLES =
      Set.of("sys_role", "sys_permission", "sys_menu", "sys_user_role", "sys_role_permission");

  @Override
  public Expression getTenantId() {
    return new LongValue(1);
  }

  @Override
  public String getTenantIdColumn() {
    return "application_id";
  }

  @Override
  public boolean ignoreTable(String table) {
    return !TABLES.contains(table.replace("`", "").toLowerCase(Locale.ROOT));
  }
}
