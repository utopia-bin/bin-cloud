package cn.utopiabin.cloud.common.json;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebJsonAutoConfigurationTest {
    @Test
    void preservesSnowflakeIdsWithoutChangingPaginationNumbers() throws Exception {
        var builder = Jackson2ObjectMapperBuilder.json();
        new WebJsonAutoConfiguration().webJsonCustomizer().customize(builder);
        var mapper = builder.build();
        var tree = mapper.readTree(mapper.writeValueAsString(Map.of(
                "id", 2_099_123_456_789_012_345L, "total", 31L,
                "operateTime", new Date(0))));
        assertThat(tree.get("id").isTextual()).isTrue();
        assertThat(tree.get("id").asText()).isEqualTo("2099123456789012345");
        assertThat(tree.get("total").isNumber()).isTrue();
        assertThat(tree.get("operateTime").asText()).isEqualTo("1970-01-01T08:00:00+08:00");
    }
}
