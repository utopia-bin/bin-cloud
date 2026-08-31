package cn.utopiabin.cloud.api.admin.handler;

import cn.utopiabin.cloud.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void exposesOnlyKnownBusinessErrorsThroughNestedRpcWrappers() {
        var response = handler.handleUnexpectedException(new RuntimeException(new BizException(409, "字典已被修改")));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMsg()).isEqualTo("字典已被修改");
        var unknown = handler.handleUnexpectedException(new RuntimeException("jdbc password=secret"));
        assertThat(unknown.getBody().getMsg()).doesNotContain("jdbc", "secret");
    }
}
