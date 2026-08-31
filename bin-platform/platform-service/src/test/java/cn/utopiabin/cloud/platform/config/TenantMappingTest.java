package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMappingTest {
    @Test
    void shouldNotSelectInheritedTenantIdFromGlobalTenantTable() {
        var table = TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "tenant-mapping-test"), Tenant.class);
        assertThat(table.getTableName()).isEqualTo("sys_tenant");
        assertThat(table.getAllSqlSelect()).doesNotContain("tenant_id");
        assertThat(table.getFieldList()).extracting("property").contains("code", "name", "available");
    }
}
