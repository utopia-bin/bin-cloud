# Bin Cloud Docker 部署

部署环境为 AlmaLinux 9.8。宿主机负责拉取代码和 Maven 构建，Docker 镜像只包含 Java 21 运行时与构建完成的 Spring Boot JAR。

## 1. 安装 Java 21 与 Maven 3.9

```bash
sudo dnf install -y git curl tar gzip java-21-openjdk-devel

MAVEN_VERSION=3.9.16
MAVEN_ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
curl -fsSLO "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}"
curl -fsSLO "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}.sha512"
echo "$(cat "${MAVEN_ARCHIVE}.sha512")  ${MAVEN_ARCHIVE}" | sha512sum --check --strict
sudo tar -xzf "${MAVEN_ARCHIVE}" -C /opt
sudo ln -sfn "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven
sudo ln -sfn /opt/maven/bin/mvn /usr/local/bin/mvn

java -version
mvn -version
docker --version
docker compose version
```

项目需要 JDK 21 和 Maven 3.9+。Docker Engine 与 Docker Compose 插件需要提前安装。

## 2. 准备运行配置

在项目根目录执行：

```bash
cp deploy/.env.example deploy/.env
vi deploy/.env
```

必须修改以下配置，不能保留 `CHANGE_ME`：

- `NACOS_PASSWORD`
- `MYSQL_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `GATEWAY_CONTEXT_SIGNING_SECRET`

可以使用 OpenSSL 生成两个不同的密钥：

```bash
openssl rand -base64 64
openssl rand -hex 32
```

如果 Nacos、MySQL 和 Redis 运行在当前 Docker 宿主机，可保留 `host.docker.internal`。Compose 已将它映射到宿主机网关。

## 3. 部署全部服务

```bash
chmod +x deploy/backend-deploy.sh
./deploy/backend-deploy.sh all
```

脚本依次执行：

1. 检查 Git 工作区与 `.env`；
2. `git fetch` 并快进到当前分支最新代码；
3. 使用 Maven Reactor 构建选中模块及其依赖；
4. 将可执行 JAR 放入 `deploy/artifacts`；
5. 构建选中服务的 Docker 镜像；
6. 只重新创建选中的容器。

默认使用 `-DskipTests`。需要在部署前运行测试时使用：

```bash
RUN_TESTS=true ./deploy/backend-deploy.sh all
```

## 4. 更新指定模块

```bash
# 只更新网关
./deploy/backend-deploy.sh bin-gateway

# 只更新管理 API
./deploy/backend-deploy.sh admin-api

# 同时更新两个 API
./deploy/backend-deploy.sh admin-api open-api

# 只更新平台服务
./deploy/backend-deploy.sh platform-service
```

| 脚本参数 | Maven 模块 | 容器 | 端口 |
| --- | --- | --- | --- |
| `bin-gateway` | `bin-gateway` | `bin-gateway` | 宿主机 `8000` |
| `admin-api` | `admin-api` | `admin-api` | Docker 内网 `8100` |
| `open-api` | `open-api` | `open-api` | Docker 内网 `8200` |
| `platform-service` | `platform-service` | `platform-service` | Docker 内网 `8300`、`20880` |

`bin-ai` 当前是依赖库集合，没有独立的 Spring Boot 启动模块，因此没有单独容器。

## 5. 运维命令

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f bin-gateway
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f platform-service
docker compose --env-file deploy/.env -f deploy/docker-compose.yml restart bin-gateway
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

前端 OpenResty 继续代理宿主机 `8000` 端口，因此后端部署完成后不需要修改现有前端 Nginx 配置。
