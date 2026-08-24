package cn.utopiabin.cloud.ai.milvus;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lazily creates and refreshes the Milvus client when connection properties change.
 */
@Slf4j
@RequiredArgsConstructor
public class MilvusClientFactory {

    private final MilvusConfig milvusConfig;

    private volatile MilvusClientV2 client;
    private volatile MilvusConfig snapshot;

    public MilvusClientV2 get() {
        if (StrUtil.isBlank(this.milvusConfig.getUri())) {
            throw new BizException("Milvus 未配置");
        }
        try {
            return this.buildClient();
        } catch (Exception exception) {
            log.error("Milvus 客户端初始化失败: {}", exception.getMessage(), exception);
            throw new BizException("Milvus 客户端初始化失败");
        }
    }

    private MilvusClientV2 buildClient() {
        if (this.client != null && this.milvusConfig.equals(this.snapshot)) {
            return this.client;
        }
        synchronized (this) {
            if (this.client != null && this.milvusConfig.equals(this.snapshot)) {
                return this.client;
            }

            ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                    .uri(this.milvusConfig.getUri())
                    .connectTimeoutMs(this.milvusConfig.getConnectTimeoutMs())
                    .rpcDeadlineMs(this.milvusConfig.getRpcDeadlineMs())
                    .keepAliveTimeMs(this.milvusConfig.getKeepAliveTimeMs())
                    .keepAliveTimeoutMs(this.milvusConfig.getKeepAliveTimeoutMs())
                    .keepAliveWithoutCalls(this.milvusConfig.isKeepAliveWithoutCalls())
                    .secure(this.milvusConfig.isSecure())
                    .enablePrecheck(this.milvusConfig.isEnablePrecheck());

            if (!StrUtil.isBlank(this.milvusConfig.getToken())) {
                builder.token(this.milvusConfig.getToken());
            } else if (!StrUtil.isBlank(this.milvusConfig.getUsername())) {
                builder.username(this.milvusConfig.getUsername())
                        .password(this.milvusConfig.getPassword());
            }
            if (!StrUtil.isBlank(this.milvusConfig.getDatabaseName())) {
                builder.dbName(this.milvusConfig.getDatabaseName());
            }

            MilvusClientV2 oldClient = this.client;
            this.client = new MilvusClientV2(builder.build());
            this.snapshot = this.milvusConfig.copyTo(MilvusConfig.class);
            this.closeQuietly(oldClient);
            log.info("Milvus 客户端初始化完成: {}, database={}",
                    this.milvusConfig.getUri(),
                    StrUtil.defaultIfBlank(this.milvusConfig.getDatabaseName(), "default"));
            return this.client;
        }
    }

    @PreDestroy
    public void close() {
        this.closeQuietly(this.client);
        this.client = null;
        this.snapshot = null;
    }

    private void closeQuietly(MilvusClientV2 target) {
        if (target == null) {
            return;
        }
        try {
            target.close();
        } catch (Exception exception) {
            log.warn("Milvus 客户端关闭异常: {}", exception.getMessage());
        }
    }
}
