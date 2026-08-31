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

## 6. 首次初始化管理员并登录

项目没有公开的默认密码。初始化默认关闭，不依赖短信服务，不需要修改前端；只在显式开启后的平台服务启动时执行，不提供公开初始化接口。

### 开启一次性初始化

先备份已有数据库，确认连接的是要初始化的数据库，并确保 V1/V2 对应的表结构已经完整建立。此功能只写初始数据，不代替 Flyway，也不会自动改变 Flyway 基线。

在服务器 `deploy/.env` 中添加或修改以下配置（下面的密码说明必须替换，不能直接作为密码使用）：

```dotenv
PLATFORM_BOOTSTRAP_ENABLED=true
PLATFORM_BOOTSTRAP_TENANT_CODE=default
PLATFORM_BOOTSTRAP_TENANT_NAME=默认租户
PLATFORM_BOOTSTRAP_USERNAME=admin
PLATFORM_BOOTSTRAP_PASSWORD='填写你自己生成的强密码'
```

密码至少 12 个字符，必须含大写字母、小写字母和数字，UTF-8 编码不超过 72 字节；同时遵守 Nacos 中现有的密码强度策略。不要把密码提交 Git、发到聊天或放进命令行参数。可用 `vi deploy/.env` 在服务器编辑，并执行 `chmod 600 deploy/.env`。Compose 单引号可以避免密码中的 `$` 被插值；不要在密码中使用单引号。

`platform.bootstrap` 的环境变量映射已放在本地 `application.yml` 中，因此现有 Nacos 配置包不需要重新导入。不要在 Nacos 写死初始密码或覆盖这些开关。当前 Compose 从 `.env` 注入环境变量。

在本次代码已推送远程后，执行：

```bash
./deploy/backend-deploy.sh platform-service
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs --tail=100 platform-service
```

看到“平台管理员初始化成功，事务已提交”日志且服务正常运行后，使用以下信息登录前端：

- 租户代码：`default`（或你设置的 `PLATFORM_BOOTSTRAP_TENANT_CODE`）
- 用户名：`admin`（或你设置的 `PLATFORM_BOOTSTRAP_USERNAME`）
- 密码：你在服务器设置的初始密码

初始化会创建租户、管理员、`super_admin` 角色、通配权限关联，以及当前前端所需的 8 个导航菜单。租户/菜单已经存在时保留原有配置；权限校验仍由后端执行，菜单不是授权依据。超级管理员不会自动绕过租户 SQL 隔离。

### 成功后关闭并移除初始密码

修改 `.env`：

```dotenv
PLATFORM_BOOTSTRAP_ENABLED=false
PLATFORM_BOOTSTRAP_PASSWORD=
```

然后重新创建平台服务容器，使环境变量生效（仅 `restart` 不会更新环境变量）：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml \
  up -d --no-deps --force-recreate platform-service
```

当前 Compose 的 `.env` 是所有后端服务共用的。如果其他容器也曾在密码存在时创建，请同时重新创建这些容器，移除其环境中的初始密码。完成后可在前端右上角修改密码。

### 幂等和故障保护

- 数据库中已存在任何 `super_admin` 角色（包含禁用/软删除状态）时，跳过管理员创建；不会重置密码、恢复角色或新增第二个超级管理员。已有孤立角色但没有可用管理员时需要人工核对，初始化不是找回密码工具。
- 目标租户存在同名账号时，拒绝初始化，不会把普通用户提升为管理员。目标租户被禁用、删除或过期时也会拒绝，不会自动修改其状态。
- 初始数据使用同一个事务写入；任一步失败全部回滚。同库并发启动通过通配权限唯一键及行锁串行化。
- 若启动提示表/字段不存在，应先核对表结构；不要删库或修改已有迁移脚本来绕过检查。

## 7. 升级字典/参数表与前端权限契约

此版本新增 `V3__system_dict_and_parameter.sql`，补齐 `sys_dict`、`sys_dict_options`、`sys_parameter`，并加入对应的 8 个权限码。已核对当前 12 个实体对应的表和持久化字段；不修改 V1/V2，不重置用户、密码或角色授权。

升级前备份数据库，并检查 `flyway_schema_history`。正常启用 Flyway 且历史版本低于 3 时，平台服务启动会执行 V3。不要将基线提升到 3 跳过该迁移；如果曾手工创建这三张表，必须先核对结构，`CREATE TABLE IF NOT EXISTS` 不会修正已有表的字段或约束。迁移已做字段覆盖和 H2 约束测试，仍需在预发布 MySQL 中验证。

前后端提交均推送远程后，先在后端仓库执行：

```bash
./deploy/backend-deploy.sh platform-service admin-api open-api
```

确认平台服务启动成功、V3 迁移成功后，再在 `bin-web` 仓库执行 `bash deploy/deploy.sh`。最后刷新前端并退出重新登录，不需要重新开启管理员初始化。

登录及当前用户接口新增 `permissionCodes`，前端页面和按钮使用这些有效权限码授权，菜单仅决定导航展示。新前端搭配旧后端会因缺少权限码而拒绝业务页面，因此必须先升级后端。持有有效 `*` 权限的管理员可访问全部业务页面；普通角色需要显式分配字典/参数权限，不会自动获得新增权限。若仍跳转 `/403`，核对当前用户接口返回的 `permissionCodes` 是否包含目标页面权限或 `*`，不要通过放开路由或给菜单填通配符绕过授权。
