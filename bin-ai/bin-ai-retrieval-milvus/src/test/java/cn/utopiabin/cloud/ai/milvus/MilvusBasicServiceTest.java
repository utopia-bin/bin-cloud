package cn.utopiabin.cloud.ai.milvus;

import cn.utopiabin.cloud.common.exception.BizException;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MilvusBasicServiceTest {

    @Test
    void rejectsNullSearchRequestBeforeAccessingClient() {
        MilvusBasicService service = new MilvusBasicService(null, null) {
            @Override
            public String getCollectionName() {
                return "image_vectors";
            }

            @Override
            public CreateCollectionReq getCreateCollectionRequest() {
                return null;
            }
        };

        assertThatThrownBy(() -> service.search(null))
                .isInstanceOf(BizException.class)
                .hasMessage("Milvus 请求为空");
    }
}
