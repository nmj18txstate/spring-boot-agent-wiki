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

## Example output

When run from a packaged build, the CLI prints deterministic progress and summary output:

```bash
java -jar target/spring-boot-agent-wiki-0.0.1-SNAPSHOT.jar --repo=.
```

Sample output from a medium Spring Boot service, not this repository:

```text
[INFO] Scanning repository target: /path/to/medium-spring-service
[INFO] Pruning skipped directories: [target, build, .git, .idea, node_modules, out, spring-boot-agent-wiki]
[INFO] Categorizing project files and processing annotation boundaries...
[INFO] Component scan complete: Found 12 Controllers, 24 Services, 18 Repositories.
[INFO] Generating deterministic markdown structures...
[SUCCESS] Generated 59 agent-ready wiki pages under spring-boot-agent-wiki/
```

The generated wiki is organized under the target repository like this:

```text
spring-boot-agent-wiki/
├── index.md
├── wiki-manifest.md
├── source-links-and-staleness.md
├── architecture/
├── spring/
├── data/
├── testing/
├── operations/
├── build/
└── agent/
```

## What is scanned

The scanner walks the repository while skipping `target/`, `build/`, `.git/`, `.idea/`, `node_modules/`, `out/`, and `spring-boot-agent-wiki/`. It inspects:

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

Phase 1.1 generates every page on every run. Optional pages are still created when no feature is detected, with guidance for future agents.

```text
spring-boot-agent-wiki/
├── index.md
├── wiki-manifest.md
├── source-links-and-staleness.md
├── architecture/
│   ├── overview.md
│   ├── request-flow.md
│   ├── package-boundaries.md
│   ├── domain-modeling.md
│   ├── dependency-map.md
│   ├── bean-dependency-map.md
│   ├── module-map.md
│   └── sequence-diagrams.md
├── spring/
│   ├── controllers.md
│   ├── services.md
│   ├── repositories.md
│   ├── configuration-properties.md
│   ├── auto-configuration-and-conditions.md
│   ├── validation.md
│   ├── dtos-and-serialization.md
│   ├── time-and-deterministic-design.md
│   ├── external-clients.md
│   ├── scheduled-jobs.md
│   ├── exception-handling.md
│   ├── security.md
│   ├── caching.md
│   ├── messaging.md
│   └── events.md
├── data/
│   ├── entities.md
│   ├── repositories.md
│   ├── migrations.md
│   ├── database-initialization.md
│   ├── transaction-boundaries.md
│   ├── query-patterns.md
│   └── data-consistency-rules.md
├── testing/
│   ├── unit-tests.md
│   ├── integration-tests.md
│   ├── test-slices.md
│   ├── deterministic-patterns.md
│   ├── testcontainers.md
│   ├── mock-patterns.md
│   └── contract-tests.md
├── operations/
│   ├── actuator.md
│   ├── health-readiness-liveness.md
│   ├── profiles.md
│   ├── logging.md
│   ├── observability.md
│   ├── graceful-shutdown.md
│   ├── virtual-threads.md
│   ├── docker.md
│   ├── kubernetes.md
│   └── native-images-and-aot.md
├── build/
│   ├── dependencies.md
│   ├── plugins.md
│   ├── packaging.md
│   └── java-version-and-runtime.md
└── agent/
    ├── coding-rules.md
    ├── safe-change-checklist.md
    ├── known-risk-areas.md
    ├── change-impact-analysis.md
    └── agent-entrypoints.md
```

## MVP detection strategy

Spring components are detected by annotation string matching, including `@RestController`, `@Service`, `@Repository`, `@Entity`, `@ConfigurationProperties`, `@Transactional`, `@Valid`, and related annotations.

Major dependencies are detected from `pom.xml` or `build.gradle` using string markers such as `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `flyway`, `liquibase`, `postgresql`, `kafka`, `rabbitmq`, `resilience4j`, and `testcontainers`.

## Roadmap

- Phase 1: deterministic static scanner.
- Phase 1.1: complete wiki page structure generated on every run.
- Phase 2: Spring AI / LLM enrichment, kept optional and layered after deterministic output.
- Phase 3: agent integration and GitHub Action automation.
