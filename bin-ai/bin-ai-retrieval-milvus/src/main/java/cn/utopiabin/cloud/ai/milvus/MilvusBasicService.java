package cn.utopiabin.cloud.ai.milvus;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.request.ReleaseCollectionReq;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.*;
import io.milvus.v2.service.vector.response.*;
import org.redisson.api.RedissonClient;

import java.util.Collections;

/**
 * Collection-scoped Milvus operations. Subclasses own their collection schema and indexes.
 */
public abstract class MilvusBasicService {

    private static final String LOCK_PREFIX = "milvus-create-collection-lock:";

    private final MilvusClientFactory milvusClientFactory;
    private final RedissonClient redissonClient;

    protected MilvusBasicService(MilvusClientFactory milvusClientFactory, RedissonClient redissonClient) {
        this.milvusClientFactory = milvusClientFactory;
        this.redissonClient = redissonClient;
    }

    public abstract String getCollectionName();

    public abstract CreateCollectionReq getCreateCollectionRequest();

    protected MilvusClientV2 getClient() {
        return this.milvusClientFactory.get();
    }

    public boolean existCollection() {
        String collectionName = this.requireCollectionName();
        return this.getClient().hasCollection(HasCollectionReq.builder()
                .collectionName(collectionName)
                .build());
    }

    public boolean createCollection() {
        String collectionName = this.requireCollectionName();
        if (this.existCollection()) {
            throw new BizException("集合已存在: " + collectionName);
        }

        var lock = this.redissonClient.getLock(LOCK_PREFIX + collectionName);
        try {
            if (!lock.tryLock()) {
                throw new BizException("操作频繁,稍后再试");
            }
            if (this.existCollection()) {
                throw new BizException("集合已存在: " + collectionName);
            }
            CreateCollectionReq request = this.getCreateCollectionRequest();
            if (request == null) {
                throw new BizException("集合配置为空");
            }
            this.requireTargetCollection(request.getCollectionName());
            this.getClient().createCollection(request);
            return true;
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void loadCollection() {
        this.getClient().loadCollection(LoadCollectionReq.builder()
                .collectionName(this.requireCollectionName()).build());
    }

    public void releaseCollection() {
        this.getClient().releaseCollection(ReleaseCollectionReq.builder()
                .collectionName(this.requireCollectionName()).build());
    }

    public void flush() {
        this.getClient().flush(FlushReq.builder()
                .collectionNames(Collections.singletonList(this.requireCollectionName())).build());
    }

    public InsertResp insert(InsertReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().insert(request);
    }

    public UpsertResp upsert(UpsertReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().upsert(request);
    }

    public SearchResp search(SearchReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().search(request);
    }

    public SearchResp hybridSearch(HybridSearchReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().hybridSearch(request);
    }

    public QueryResp query(QueryReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().query(request);
    }

    public GetResp get(GetReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().get(request);
    }

    public DeleteResp delete(DeleteReq request) {
        this.requireRequest(request);
        this.requireTargetCollection(request.getCollectionName());
        return this.getClient().delete(request);
    }

    private String requireCollectionName() {
        String collectionName = this.getCollectionName();
        if (StrUtil.isBlank(collectionName)) {
            throw new BizException("集合名称为空");
        }
        return collectionName;
    }

    private void requireRequest(Object request) {
        if (request == null) {
            throw new BizException("Milvus 请求为空");
        }
    }

    private void requireTargetCollection(String targetCollectionName) {
        String collectionName = this.requireCollectionName();
        if (!collectionName.equals(targetCollectionName)) {
            throw new BizException("请求集合与当前服务不一致: " + targetCollectionName);
        }
    }
}
