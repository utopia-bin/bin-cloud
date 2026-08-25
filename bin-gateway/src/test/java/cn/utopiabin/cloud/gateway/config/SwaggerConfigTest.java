package cn.utopiabin.cloud.gateway.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwaggerConfigTest {

    @Test
    void exposesStableGatewayDocumentUrlsForKnownServices() {
        var urls = new SwaggerConfig().swaggerUrls().stream()
                .collect(Collectors.toMap(url -> url.getName(), url -> url.getUrl()));

        assertEquals(Map.of(
                "admin-api", "/admin/v3/api-docs",
                "open-api", "/open/v3/api-docs",
                "platform-service", "/platform/v3/api-docs"
        ), urls);
    }
}
