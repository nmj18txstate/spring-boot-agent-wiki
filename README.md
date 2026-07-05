# spring-boot-agent-wiki

`spring-boot-agent-wiki` is a Java 21 / Spring Boot CLI-style tool that scans a Spring Boot repository and generates an agent-ready Markdown wiki in a `spring-boot-agent-wiki/` folder inside the scanned repository.

The MVP is intentionally deterministic: it performs static analysis only, uses annotation and dependency string matching, and does not call an LLM, Spring AI, Docker, databases, or external services.

## Why it exists

Coding agents need a concise, source-linked map of a codebase before making changes. This tool produces a repeatable wiki that highlights Spring components, major dependencies, operational concerns, testing patterns, and safe-change guidance.

## Usage

Compile and run with Java 21:

```bash
mkdir -p out/main && javac --release 21 -d out/main $(find src/main/java -name '*.java') && java -cp out/main dev.agentwiki.SpringBootAgentWikiApplication --repo=/path/to/spring-boot/repo
```

If `--repo` is omitted, the current working directory is scanned:

```bash
java -cp out/main dev.agentwiki.SpringBootAgentWikiApplication
```

The generated files are written only under:

```text
/path/to/spring-boot/repo/spring-boot-agent-wiki/
```

## What is scanned

The scanner walks the repository while skipping `target/`, `build/`, `.git/`, `.idea/`, and `node_modules/`. It inspects:

- `src/main/java`
- `src/test/java`
- `src/main/resources`
- `db/migration`
- `pom.xml`
- `build.gradle`
- `Dockerfile`
- `docker-compose.yml`
- Kubernetes YAML files when paths indicate Kubernetes or k8s usage

## Generated wiki structure

```text
spring-boot-agent-wiki/
  index.md
  wiki-manifest.md
  source-links-and-staleness.md
  architecture/
  spring/
  data/
  testing/
  operations/
  build/
  agent/
```

Key pages include architecture overview, request flow, package boundaries, controllers, services, repositories, entities, migrations, transactions, deterministic test patterns, Java 21 virtual threads, dependency summaries, safe-change checklists, and coding rules for agents.

## MVP detection strategy

Spring components are detected by annotation string matching, including `@RestController`, `@Service`, `@Repository`, `@Entity`, `@ConfigurationProperties`, `@Transactional`, `@Valid`, and related annotations.

Major dependencies are detected from `pom.xml` or `build.gradle` using string markers such as `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `flyway`, `liquibase`, `postgresql`, `kafka`, `rabbitmq`, `resilience4j`, and `testcontainers`.

## Roadmap

- Add richer Java parsing for method-level routes, bean graphs, and package ownership.
- Add Gradle Kotlin DSL detection.
- Add optional integration with LLM/Spring AI after deterministic foundations are stable.
- Add incremental generation and staleness checks.
- Add SARIF or JSON export for downstream agent tools.
