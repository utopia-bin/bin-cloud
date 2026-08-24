## 技术栈

| 领域      | 技术                                   | 版本       |
|---------|--------------------------------------|----------|
| 基础框架    | Spring Boot                          | 3.5.16   |
| 微服务     | Spring Cloud + Spring Cloud Alibaba  | 2025.0.x |
| RPC     | Apache Dubbo (Spring Boot 3 适配)      | 3.3.6    |
| 注册/配置中心 | Nacos                                | -        |
| ORM     | MyBatis Plus                         | 3.5.16   |
| 缓存      | Redis (Redisson + Spring Data Redis) | 3.40.2   |
| 搜索引擎    | Elasticsearch                        | 8.18.3   |
| AI 模型抽象  | Spring AI                            | 1.1.7    |
| 向量数据库   | Milvus                               | 2.6.x    |
| JSON    | Fastjson2                            | 2.0.57   |
| JDK     | Java                                 | 21       |


## 服务端口

| 服务                 | 端口   | 服务名                  |
|--------------------|------|----------------------|
| bin-gateway        | 8000 | `bin-gateway`        |
| admin-api          | 8100 | `admin-api`          |
| open-api           | 8200 | `open-api`           |
| platform-service   | 8300 | `platform-service`   |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.0+
- Elasticsearch 8.x
- Milvus 2.6+（使用向量检索能力时需要）
- Nacos 2.x
