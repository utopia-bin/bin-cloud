# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Maven reactor for Spring Boot/Spring Cloud services. The root `pom.xml` manages shared versions and builds four top-level modules:

- `bin-common`: shared DTOs, utilities, context, Redis, Elasticsearch, and Milvus support.
- `bin-gateway`: API gateway service (port 8000).
- `bin-api`: parent for `admin-api` (8100) and `open-api` (8200).
- `bin-platform`: parent for the reusable `platform-api` contract and `platform-service` implementation (8300).
- `bin-ai`: parent for the lightweight `ai-api` Dubbo contracts and the `ai-service` implementation (8400).

Production code belongs in `src/main/java`, configuration in `src/main/resources`, and tests in `src/test/java` with the same package hierarchy. Do not commit generated `target/` or runtime `logs/` content.

## Build, Test, and Development Commands

Use JDK 21 and Maven 3.9+ from the repository root.

```bash
mvn clean verify
mvn -pl bin-common -am test
mvn -pl :platform-service -am spring-boot:run
mvn -pl :bin-gateway -am spring-boot:run
```

`clean verify` compiles and tests the full reactor. `-pl ... -am` limits work to one module plus its dependencies. Local services rely on Nacos configuration and may also require MySQL, Redis, Elasticsearch, and Milvus.

## Coding Style & Naming Conventions

Use four-space indentation, UTF-8, one public type per file, and package names under `cn.utopiabin.cloud`. Follow existing suffixes: `DTO`, `VO`, `Repository`, `Service`, `Controller`, `Config`, and `AutoConfiguration`. Use PascalCase for types, camelCase for members, and UPPER_SNAKE_CASE for constants. Prefer constructor injection and existing Lombok patterns such as `@RequiredArgsConstructor` and `@Slf4j`. No formatter or lint plugin is enforced; keep imports organized and run `mvn verify` before submitting.

提交前必须格式化代码并清理无效导入、通配符导入和不必要的全限定类名；仅在类名确实冲突时使用全限定类名。业务注释和 Javadoc 统一使用中文，关键边界、状态转换和异常分支必须说明原因。持久层统一使用 MyBatis-Plus；简单单表操作使用实体、Mapper 和条件构造器，复杂 SQL 放在 Mapper XML 中。Service 只负责业务编排，数据库访问和结果映射必须放在按领域拆分的 Repository 中，禁止在 Service 或 Controller 中注入 `JdbcTemplate`、`SqlSessionTemplate`，也禁止通过字符串 Mapper statement 名称访问数据库。后端功能权限统一使用 `@RequirePermission`，只有注解无法表达的租户隔离或资源归属校验才允许编写边界判断，并使用中文说明原因。

### CRUD 功能模板

新增或重构 CRUD 功能时，以 `platform-service` 的系统字典管理为结构模板：实体按领域放入 `entity/<domain>` 并继承现有基础实体；Mapper 与实体一一对应、继承 `BaseMapper<T>`，只保留该聚合确实需要的复杂查询；Repository 按领域封装 MyBatis-Plus 条件构造器、分页、唯一性检查、持久化和结果映射；Service 仅做校验、权限、事务与业务编排；API 实现只委托 Service。DTO、Query、VO 分离，禁止以 `Map<String, Object>`、`Object[]`、位置参数 `p0/p1` 或通用万能模型代替明确的请求、查询和持久化类型。

一个 Mapper/XML 只能服务一个清晰聚合或查询边界。禁止创建被多个无关 Repository 共享的“万能 Mapper”，也禁止在单个 XML 中混放目录、授权、会话、审计等不同领域 SQL。简单单表 CRUD 不得重复手写 XML；确需 XML 的复杂 SQL 必须显式列出字段、使用有业务含义的参数名与具体 `resultType/resultMap`，禁止 `SELECT *`、`resultType="map"` 和无法表达含义的序号参数。包路径必须表达领域归属，`service`、`repository`、`mapper`、`entity`、`api.impl` 下的业务类均放入对应领域子包，禁止将业务 Service 或 Controller 裸放在根包。

All API contract models, including DTOs, VOs, query objects, pagination wrappers, and other request or response models, must provide Swagger/OpenAPI documentation with `io.swagger.v3.oas.annotations.media.Schema`. Add a clear, business-oriented `@Schema(description = "...")` to every model type and every externally visible field. Document value ranges, formats, enum meanings, defaults, required status, and representative examples where they improve the generated API contract; do not use vague descriptions that merely repeat the Java field name.

## Testing Guidelines

Spring Boot Test provides JUnit 5. Add focused unit tests named `*Test.java`; use `*IT.java` for tests requiring infrastructure. Mirror the production package and avoid relying on shared mutable services. There is currently no coverage threshold, so prioritize authentication, tenant isolation, persistence queries, and reusable common components.

为当前修改临时新增的单元测试或集成测试只用于本地验证：验证通过后必须删除这些临时测试文件。测试过程中生成的 Markdown 报告、日志、快照和其他临时产物也必须同步删除，不得遗留在源码树或提交中；长期回归测试仅在用户明确要求保留，或该测试原本已属于仓库正式测试集时保留。删除前必须确认目标确为本次测试产生的文件，不得清理用户已有文件或历史测试。

## Commit & Pull Request Guidelines

History uses concise prefixes such as `feat:`, `fix:`, and `init:`. Git 提交说明必须使用中文，例如 `refactor: 重构应用持久层`。Keep each commit scoped to one concern. Pull requests should explain the change, affected modules, configuration or migration requirements, and verification commands. Link relevant issues and include request/response examples for API changes.

Do not create Git commits unless the user explicitly instructs you to commit the current changes. Requests to implement, fix, verify, review, finish, or submit work do not by themselves authorize a commit. When commit authorization is absent, leave the verified changes in the working tree and report that they remain uncommitted.

## Security & Configuration

Keep credentials out of YAML and source files. Use environment placeholders or Nacos-managed secrets. Never log tokens, passwords, API keys, or raw personal data.
