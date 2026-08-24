package cn.utopiabin.cloud.ai.elasticsearch;

import cn.utopiabin.cloud.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ESBasicServiceTest {

    @Test
    void rejectsMissingIndexConfigurationBeforeAccessingClients() {
        ESBasicService service = new ESBasicService(null, null) {
            @Override
            public ESCreateIndexRequest getCreateIndexRequest() {
                return null;
            }
        };

        assertThatThrownBy(service::createIndex)
                .isInstanceOf(BizException.class)
                .hasMessage("索引名称为空");
    }
}
