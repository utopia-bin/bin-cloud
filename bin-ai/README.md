# bin-ai

`bin-ai` 是可按需引入的 AI 工具库，不是独立启动的微服务。父模块只负责聚合和版本管理，不包含业务代码。

| 模块 | 能力 | 主要传递依赖 |
|---|---|---|
| `bin-ai-core` | 厂商无关的模型抽象和注册中心 | Spring AI Model |
| `bin-ai-chat-tencent` | 腾讯 LKE `ChatModel` 适配 | `bin-ai-core`、WebFlux |
| `bin-ai-retrieval-milvus` | Milvus 集合管理和向量检索 | Milvus SDK、Redisson |
| `bin-ai-search-elasticsearch` | ES 索引管理和查询构建 | Elasticsearch Client、Redisson |

## 按需引入

版本由根项目统一管理，业务模块不需要填写版本：

```xml
<!-- 只使用腾讯对话，同时自动获得 bin-ai-core -->
<dependency>
    <groupId>cn.utopiabin</groupId>
    <artifactId>bin-ai-chat-tencent</artifactId>
</dependency>

<!-- 只使用 Milvus，不会引入 Spring AI、WebFlux 或 Elasticsearch -->
<dependency>
    <groupId>cn.utopiabin</groupId>
    <artifactId>bin-ai-retrieval-milvus</artifactId>
</dependency>

<!-- 只使用 Elasticsearch，不会引入 Spring AI、WebFlux 或 Milvus -->
<dependency>
    <groupId>cn.utopiabin</groupId>
    <artifactId>bin-ai-search-elasticsearch</artifactId>
</dependency>
```

不要把聚合父模块 `bin-ai` 作为普通 JAR 依赖；它的 packaging 为 `pom`。

## 腾讯对话配置

```yaml
bin:
  ai:
    providers:
      tencent:
        enabled: true
        app-key: ${TENCENT_LKE_APP_KEY}
        defaults:
          model: ${TENCENT_LKE_MODEL:}
```

业务代码通过 `ChatModelRegistry` 按厂商标识获取模型，避免直接依赖腾讯实现：

```java
ChatModel chatModel = chatModelRegistry.get("tencent").chatModel();
```

## Milvus 配置

```yaml
milvus:
  uri: ${MILVUS_URI}
  token: ${MILVUS_TOKEN:}
  database-name: ${MILVUS_DATABASE:}
```

未配置 `milvus.uri` 时不会创建 `MilvusClientFactory`，也不会主动连接 Milvus。

## Elasticsearch 配置

```yaml
es:
  api-url: ${ES_API_URL}
  api-key: ${ES_API_KEY:}
```

未配置 `es.api-url` 时不会创建 `ESClientFactory`。配置变更后客户端会延迟重建，旧客户端会被安全关闭。
