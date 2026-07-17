---
name: spring-boot-agent-wiki
description: Use when working in a Spring Boot repository and you need to install or run the Spring Boot Agent Wiki CLI, generate or refresh the `spring-boot-agent-wiki/` markdown wiki, and consult source-linked architecture, Spring layer, data, testing, operations, build, and safe-change guidance before editing code.
allowed-tools: Bash Read Grep Glob
license: MIT
---

# Spring Boot Agent Wiki

Use this skill when modifying a Spring Boot repository and you need durable, source-linked project context before changing code.

The Spring Boot Agent Wiki CLI scans a target Spring Boot repository and generates an agent-ready markdown wiki under:

```text
spring-boot-agent-wiki/
```

The generated wiki helps agents avoid repeatedly rediscovering architecture from raw source files. It gives a repeatable workflow:

```text
install pinned CLI release
  -> run against target repository
  -> read relevant generated wiki pages
  -> modify code safely
  -> refresh wiki after structural changes
```

## When to use this skill

Use this skill when the task involves any of the following in a Spring Boot repository:

- Understanding project architecture before editing code
- Changing controllers, services, repositories, entities, DTOs, configuration, or tests
- Reviewing service-layer or business-logic changes
- Changing database migrations, transaction boundaries, query patterns, or consistency rules
- Changing YAML, Docker, Kubernetes, build files, README, or operational configuration
- Preparing AI coding agents with source-linked repository context
- Refreshing generated wiki pages after code changes

Do not use this skill as a replacement for reading the touched source code. The generated wiki is a navigation and safety layer; the application source remains the source of truth.

---

# Installation

Install the CLI outside the target project you want to scan.

Use the pinned `v1.0.0` release unless you intentionally need to build the tool from source.

## Option A: Download the pinned release JAR

Recommended version:

```text
v1.0.0
```

Unix/macOS:

```bash
curl -L -o spring-boot-agent-wiki-1.0.0.jar https://github.com/nmj18txstate/spring-boot-agent-wiki/releases/download/v1.0.0/spring-boot-agent-wiki-1.0.0.jar
```

Windows CMD:

```bat
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://github.com/nmj18txstate/spring-boot-agent-wiki/releases/download/v1.0.0/spring-boot-agent-wiki-1.0.0.jar' -OutFile 'spring-boot-agent-wiki-1.0.0.jar'"
```

Verify the downloaded JAR:

Unix/macOS:

```bash
java -jar spring-boot-agent-wiki-1.0.0.jar --repo=/path/to/target-spring-boot-repo
```

Windows CMD:

```bat
java -jar spring-boot-agent-wiki-1.0.0.jar --repo=C:\path\to\target-spring-boot-repo
```

Expected output includes:

```text
[INFO] Scanning repository target: /path/to/target-spring-boot-repo
[INFO] Pruning skipped directories: [target, build, .git, .idea, node_modules, out, spring-boot-agent-wiki]
[INFO] Categorizing project files and processing annotation boundaries...
[INFO] Component scan complete: Found <n> Controllers, <n> Services, <n> Repositories.
[INFO] Generating deterministic markdown structures...
[SUCCESS] Generated <n> agent-ready wiki pages under spring-boot-agent-wiki/
```

## Option B: Build the pinned release from source

Use this only when a local source build is needed.

Unix/macOS:

```bash
git clone https://github.com/nmj18txstate/spring-boot-agent-wiki.git
cd spring-boot-agent-wiki
git checkout v1.0.0
mvn clean package
```

Windows CMD:

```bat
git clone https://github.com/nmj18txstate/spring-boot-agent-wiki.git
cd spring-boot-agent-wiki
git checkout v1.0.0
mvn clean package
```

The runnable JAR is created at:

```text
target/spring-boot-agent-wiki-1.0.0.jar
```

Verify the source build:

Unix/macOS:

```bash
java -cp target/classes:target/test-classes dev.agentwiki.TestRunner
java -jar target/spring-boot-agent-wiki-1.0.0.jar --repo=.
```

Windows CMD:

```bat
java -cp target\classes;target\test-classes dev.agentwiki.TestRunner
java -jar target\spring-boot-agent-wiki-1.0.0.jar --repo=.
```

Expected output includes:

```text
All tests passed
[SUCCESS] Generated ... agent-ready wiki pages under spring-boot-agent-wiki/
```

---

# Run against a target Spring Boot repository

The `--repo` argument must point to the target repository you want to scan.

Do not assume the tool repository and target repository are the same.

## Unix/macOS

```bash
java -jar /path/to/spring-boot-agent-wiki-1.0.0.jar --repo=/path/to/target-spring-boot-repo
```

## Windows CMD

```bat
java -jar C:\path\to\spring-boot-agent-wiki-1.0.0.jar --repo=C:\path\to\target-spring-boot-repo
```

The tool writes generated markdown only under the target repository's:

```text
spring-boot-agent-wiki/
```

It skips common generated/tooling directories such as:

```text
target/
build/
.git/
.idea/
node_modules/
out/
spring-boot-agent-wiki/
```

---

# Required agent workflow

Before editing code in the target Spring Boot repository, follow this workflow.

## 1. Generate or refresh the wiki

Run the pinned CLI release against the target repo.

Unix/macOS:

```bash
java -jar /path/to/spring-boot-agent-wiki-1.0.0.jar --repo=/path/to/target-spring-boot-repo
```

Windows CMD:

```bat
java -jar C:\path\to\spring-boot-agent-wiki-1.0.0.jar --repo=C:\path\to\target-spring-boot-repo
```

## 2. Start with the generated entrypoints

Open these files first:

```text
spring-boot-agent-wiki/index.md
spring-boot-agent-wiki/agent/agent-entrypoints.md
spring-boot-agent-wiki/agent/coding-rules.md
spring-boot-agent-wiki/agent/safe-change-checklist.md
```

Do not load the entire `spring-boot-agent-wiki/` directory into context at once. Use `agent/agent-entrypoints.md` to select only the pages relevant to the task.

## 3. Load pages based on change type

For API changes, read:

```text
spring-boot-agent-wiki/spring/controllers.md
spring-boot-agent-wiki/spring/dtos-and-serialization.md
spring-boot-agent-wiki/spring/validation.md
spring-boot-agent-wiki/spring/exception-handling.md
```

For service or business-logic changes, read:

```text
spring-boot-agent-wiki/spring/services.md
spring-boot-agent-wiki/architecture/request-flow.md
spring-boot-agent-wiki/data/transaction-boundaries.md
spring-boot-agent-wiki/agent/coding-rules.md
spring-boot-agent-wiki/agent/safe-change-checklist.md
```

For database changes, read:

```text
spring-boot-agent-wiki/data/entities.md
spring-boot-agent-wiki/data/repositories.md
spring-boot-agent-wiki/data/migrations.md
spring-boot-agent-wiki/data/query-patterns.md
spring-boot-agent-wiki/data/data-consistency-rules.md
```

For configuration or property changes, read:

```text
spring-boot-agent-wiki/spring/configuration-properties.md
spring-boot-agent-wiki/spring/auto-configuration-and-conditions.md
spring-boot-agent-wiki/operations/profiles.md
```

For time-dependent logic, read:

```text
spring-boot-agent-wiki/spring/time-and-deterministic-design.md
spring-boot-agent-wiki/testing/deterministic-patterns.md
```

Prefer injected `java.time.Clock` over direct calls to:

```text
Instant.now()
LocalDate.now()
System.currentTimeMillis()
```

For testing changes, read:

```text
spring-boot-agent-wiki/testing/unit-tests.md
spring-boot-agent-wiki/testing/integration-tests.md
spring-boot-agent-wiki/testing/test-slices.md
spring-boot-agent-wiki/testing/testcontainers.md
spring-boot-agent-wiki/testing/mock-patterns.md
```

For operations, Docker, Kubernetes, native image, or observability changes, read:

```text
spring-boot-agent-wiki/operations/actuator.md
spring-boot-agent-wiki/operations/health-readiness-liveness.md
spring-boot-agent-wiki/operations/logging.md
spring-boot-agent-wiki/operations/observability.md
spring-boot-agent-wiki/operations/docker.md
spring-boot-agent-wiki/operations/kubernetes.md
spring-boot-agent-wiki/operations/native-images-and-aot.md
```

For build changes, read:

```text
spring-boot-agent-wiki/build/dependencies.md
spring-boot-agent-wiki/build/plugins.md
spring-boot-agent-wiki/build/packaging.md
spring-boot-agent-wiki/build/java-version-and-runtime.md
```

---

# Safe-change guardrails

## Avoid stale wiki drift

If you change Spring Boot structure, refresh the wiki before finishing.

Refresh after changing:

- Controllers
- Services
- Repositories
- Entities
- DTOs
- Configuration properties
- YAML/properties files
- Migrations
- Tests
- Dockerfiles
- Kubernetes manifests
- Build files
- README or architecture documentation

Run the pinned CLI release again.

Unix/macOS:

```bash
java -jar /path/to/spring-boot-agent-wiki-1.0.0.jar --repo=/path/to/target-spring-boot-repo
```

Windows CMD:

```bat
java -jar C:\path\to\spring-boot-agent-wiki-1.0.0.jar --repo=C:\path\to\target-spring-boot-repo
```

Then inspect the generated wiki diff.

## Treat source code as source of truth

The generated wiki helps navigate and summarize the repository, but it must not override the actual source code, tests, build files, or configuration.

When the wiki and source disagree:

1. Trust the source.
2. Update the code if the code is wrong.
3. Regenerate the wiki if the wiki is stale.

## Preserve Spring Boot layering

Agents must avoid generating controller-heavy or repository-heavy designs.

Expected layering:

```text
Controller
  -> Service
  -> Repository / External Client
  -> Database / External System
```

Rules:

- Controllers handle HTTP mapping and request validation.
- Services own business decisions and orchestration.
- Repositories own persistence queries.
- External clients own outbound API integration.
- DTOs and Java records should not be confused with JPA entities.
- Do not return JPA entities directly from REST controllers.

## Preserve idempotent state transitions

For stateful workflows, agents must not blindly trigger side effects.

Before starting backup, restore, cleanup, expansion, failover, or other mutation steps, check the current state.

Preferred pattern:

```java
if (state == RemediationStatus.PENDING_BACKUP) {
    startBackup();
}
```

Avoid:

```java
startBackup();
```

When retries are possible, store external operation IDs and make repeated calls safe.

## Preserve Java record compatibility

When changing Java records used as DTOs or domain snapshots:

- Do not add new required fields without considering old payloads.
- Add defaults for newly introduced optional fields.
- Use compact constructors or explicit `@JsonCreator` mapping when compatibility matters.
- Add serialization/deserialization tests when record components change.

---

# Final verification before completing a task

Before finalizing a code change in the target repository:

1. Run the project's normal tests.
2. Rerun the pinned Spring Boot Agent Wiki CLI release.
3. Review the generated wiki diff.
4. Confirm the changed layer's wiki pages still match the source.
5. Mention whether wiki files were regenerated or intentionally left unchanged.

Example final commands:

Unix/macOS:

```bash
java -jar /path/to/spring-boot-agent-wiki-1.0.0.jar --repo=/path/to/target-spring-boot-repo
```

Windows CMD:

```bat
java -jar C:\path\to\spring-boot-agent-wiki-1.0.0.jar --repo=C:\path\to\target-spring-boot-repo
```

Expected success output:

```text
[INFO] Scanning repository target: /path/to/target-spring-boot-repo
[INFO] Pruning skipped directories: [target, build, .git, .idea, node_modules, out, spring-boot-agent-wiki]
[INFO] Categorizing project files and processing annotation boundaries...
[INFO] Component scan complete: Found <n> Controllers, <n> Services, <n> Repositories.
[INFO] Generating deterministic markdown structures...
[SUCCESS] Generated <n> agent-ready wiki pages under spring-boot-agent-wiki/
```