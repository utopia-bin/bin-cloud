package cn.utopiabin.cloud.common.es;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import co.elastic.clients.elasticsearch.indices.TranslogDurability;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

/**
 * ES 基础服务 (抽象类,子类实现 {@link #getCreateIndexRequest()} 并注入依赖)
 *
 * @since 1.0
 */
@Slf4j
public abstract class ESBasicService {

    private static final String LOCK_PREFIX = "es-create-index-lock:";

    private final ESClientFactory esClientFactory;
    private final RedissonClient redissonClient;

    protected ESBasicService(ESClientFactory esClientFactory, RedissonClient redissonClient) {
        this.esClientFactory = esClientFactory;
        this.redissonClient = redissonClient;
    }

    /**
     * 子类提供索引配置
     */
    public abstract ESCreateIndexRequest getCreateIndexRequest();

    protected ElasticsearchClient getClient() {
        return esClientFactory.get();
    }

    /**
     * 判断索引是否存在
     */
    public boolean existIndex() throws Exception {
        var bo = getCreateIndexRequest();
        if (bo == null) {
            return false;
        }
        return getClient().indices().exists(c -> c.index(indexName(bo))).value();
    }

    /**
     * 创建索引
     */
    public boolean createIndex() throws Exception {
        var bo = getCreateIndexRequest();
        if (bo == null || StrUtil.isBlank(bo.getName())) {
            throw new BizException("索引名称为空");
        }
        if (existIndex()) {
            throw new BizException("索引已存在: " + bo.getName());
        }

        var lock = redissonClient.getLock(LOCK_PREFIX + bo.getName());
        try {
            if (!lock.tryLock()) {
                throw new BizException("操作频繁,稍后再试");
            }
            var indexName = indexName(bo);
            var req = new CreateIndexRequest.Builder().index(indexName);
            if (!indexName.equals(bo.getName())) {
                req.aliases(bo.getName(), a -> a.isWriteIndex(true));
            }
            if (bo.getMapping() != null) {
                req.mappings(bo.getMapping());
            }
            req.settings(s -> s
                    .refreshInterval(r -> r.time(bo.getRefreshInterval()))
                    .numberOfShards(String.valueOf(bo.getShards()))
                    .maxResultWindow(20000)
                    .numberOfReplicas(String.valueOf(bo.getReplicas()))
                    .translog(t -> t.durability(TranslogDurability.Request))
                    .analysis(analysis())
            );
            return getClient().indices().create(req.build()).acknowledged();
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private IndexSettingsAnalysis analysis() {
        return IndexSettingsAnalysis.of(a -> a
                .analyzer(ESAnalyzer.IK_SMART, aa -> aa.custom(c -> c.tokenizer(ESAnalyzer.IK_SMART)))
                .analyzer(ESAnalyzer.IK_MAX_WORD, aa -> aa.custom(c -> c.tokenizer(ESAnalyzer.IK_MAX_WORD)))
                .analyzer(ESAnalyzer.NGRAM, aa -> aa.custom(c -> c.tokenizer(ESAnalyzer.NGRAM)))
                .analyzer(ESAnalyzer.COMMA, aa -> aa.pattern(p -> p.pattern(",")))
                .analyzer(ESAnalyzer.SEMICOLON, aa -> aa.pattern(p -> p.pattern(";")))
        );
    }

    private String indexName(ESCreateIndexRequest bo) {
        return bo.getName() + StrUtil.defaultIfBlank(bo.getVersion(), "");
    }
}
