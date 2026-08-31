package cn.utopiabin.cloud.platform.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import java.util.Set;

/** Existing IAM mappers are console-only. Cross-application APIs use explicitly scoped JDBC. */
public class ConsoleApplicationLineHandler implements TenantLineHandler {
    private static final Set<String> TABLES = Set.of("sys_role","sys_permission","sys_menu","sys_user_role","sys_role_permission");
    @Override public Expression getTenantId() { return new LongValue(1); }
    @Override public String getTenantIdColumn() { return "application_id"; }
    @Override public boolean ignoreTable(String table) { return !TABLES.contains(table.replace("`", "").toLowerCase(java.util.Locale.ROOT)); }
}
