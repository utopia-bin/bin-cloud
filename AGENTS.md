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

All API contract models, including DTOs, VOs, query objects, pagination wrappers, and other request or response models, must provide Swagger/OpenAPI documentation with `io.swagger.v3.oas.annotations.media.Schema`. Add a clear, business-oriented `@Schema(description = "...")` to every model type and every externally visible field. Document value ranges, formats, enum meanings, defaults, required status, and representative examples where they improve the generated API contract; do not use vague descriptions that merely repeat the Java field name.

## Testing Guidelines

Spring Boot Test provides JUnit 5. Add focused unit tests named `*Test.java`; use `*IT.java` for tests requiring infrastructure. Mirror the production package and avoid relying on shared mutable services. There is currently no coverage threshold, so prioritize authentication, tenant isolation, persistence queries, and reusable common components.

## Commit & Pull Request Guidelines

History uses concise prefixes such as `feat:`, `fix:`, and `init:`; continue that pattern, for example `feat: add Milvus collection service`. Keep each commit scoped to one concern. Pull requests should explain the change, affected modules, configuration or migration requirements, and verification commands. Link relevant issues and include request/response examples for API changes.

## Security & Configuration

Keep credentials out of YAML and source files. Use environment placeholders or Nacos-managed secrets. Never log tokens, passwords, API keys, or raw personal data.
