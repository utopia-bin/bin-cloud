package cn.utopiabin.cloud.ai.elasticsearch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ESQueryTest {

    @Test
    void buildsTermsQuery() {
        var query = ESQuery.terms("tenantId", List.of(1L, 2L));

        assertThat(query.isTerms()).isTrue();
        assertThat(query.terms().field()).isEqualTo("tenantId");
        assertThat(query.terms().terms().value()).hasSize(2);
    }

    @Test
    void indexRequestHasSafeDefaults() {
        ESCreateIndexRequest request = new ESCreateIndexRequest().setName("documents");

        assertThat(request.getShards()).isEqualTo(1);
        assertThat(request.getReplicas()).isEqualTo(1);
        assertThat(request.getRefreshInterval()).isEqualTo("1s");
    }
}
