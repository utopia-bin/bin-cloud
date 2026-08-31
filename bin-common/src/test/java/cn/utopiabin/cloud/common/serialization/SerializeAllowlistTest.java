package cn.utopiabin.cloud.common.serialization;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.rest.RestResult;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.hessian2.Hessian2Serialization;
import org.apache.dubbo.common.utils.DefaultSerializeClassChecker;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SerializeAllowlistTest {

    @Test
    void shouldLoadClasspathAllowlistAndTransportBusinessExceptionsWithoutApiRegistration() throws Exception {
        var framework = new FrameworkModel();
        try {
            var module = framework.newApplication().newModule();
            var manager = framework.getBeanFactory().getBean(SerializeSecurityManager.class);
            manager.setCheckStatus(SerializeCheckStatus.STRICT);
            manager.setCheckSerializable(true);
            Set<String> allowed = ReflectionTestUtils.invokeMethod(manager, "getAllowedPrefix");
            assertThat(allowed).contains(BizException.class.getName())
                    .doesNotContain(RestResult.class.getName());

            var checker = framework.getBeanFactory().getBean(DefaultSerializeClassChecker.class);
            assertThat(checker.isCheckSerializable()).isTrue();
            // HTTP response wrappers are not implicitly trusted as RPC payloads.
            assertThatThrownBy(() -> checker.loadClass(getClass().getClassLoader(), RestResult.class.getName()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not in allow list");

            URL url = new URL("dubbo", "127.0.0.1", 20880);
            url.setScopeModel(module);
            var serializer = new Hessian2Serialization();
            var bytes = new ByteArrayOutputStream();
            var output = serializer.serialize(url, bytes);
            output.writeThrowable(new BizException(409, "test conflict"));
            output.flushBuffer();
            var input = serializer.deserialize(url, new ByteArrayInputStream(bytes.toByteArray()));
            var exception = (BizException) input.readThrowable();
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("test conflict");
        } finally {
            framework.destroy();
        }
    }
}
