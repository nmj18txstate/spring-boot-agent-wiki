package dev.agentwiki.markdown;

import dev.agentwiki.scanner.ComponentType;
import dev.agentwiki.scanner.DependencyType;
import dev.agentwiki.scanner.DetectedComponent;
import dev.agentwiki.scanner.FileCategory;
import dev.agentwiki.scanner.RepositoryScan;
import dev.agentwiki.scanner.ScannedFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MarkdownWikiGenerator {

    private static final List<String> WIKI_FILES = List.of(
            "index.md", "wiki-manifest.md", "source-links-and-staleness.md",
            "architecture/overview.md", "architecture/request-flow.md", "architecture/package-boundaries.md", "architecture/domain-modeling.md", "architecture/bean-dependency-map.md",
            "spring/controllers.md", "spring/services.md", "spring/repositories.md", "spring/configuration-properties.md", "spring/validation.md", "spring/dtos-and-serialization.md", "spring/time-and-deterministic-design.md", "spring/exception-handling.md",
            "data/entities.md", "data/migrations.md", "data/transaction-boundaries.md", "data/data-consistency-rules.md",
            "testing/test-slices.md", "testing/deterministic-patterns.md", "testing/testcontainers.md",
            "operations/actuator.md", "operations/health-readiness-liveness.md", "operations/profiles.md", "operations/logging.md", "operations/observability.md", "operations/graceful-shutdown.md", "operations/virtual-threads.md",
            "build/dependencies.md", "build/java-version-and-runtime.md",
            "agent/coding-rules.md", "agent/safe-change-checklist.md", "agent/known-risk-areas.md", "agent/change-impact-analysis.md", "agent/agent-entrypoints.md");

    public void generate(RepositoryScan scan, Path wikiRoot) throws IOException {
        Files.createDirectories(wikiRoot);
        for (String wikiFile : WIKI_FILES) {
            Path output = wikiRoot.resolve(wikiFile).normalize();
            if (!output.startsWith(wikiRoot.normalize())) {
                throw new IOException("Refusing to write outside wiki root: " + output);
            }
            Files.createDirectories(output.getParent() == null ? wikiRoot : output.getParent());
            Files.writeString(output, contentFor(wikiFile, scan), StandardCharsets.UTF_8);
        }
    }

    private String contentFor(String file, RepositoryScan scan) {
        return switch (file) {
            case "index.md" -> index(scan);
            case "wiki-manifest.md" -> manifest(scan);
            case "source-links-and-staleness.md" -> sourceLinks(scan);
            case "spring/controllers.md" -> componentPage("Controllers", scan, c -> c.type() == ComponentType.REST_CONTROLLER || c.type() == ComponentType.CONTROLLER);
            case "spring/services.md" -> componentPage("Services", scan, c -> c.type() == ComponentType.SERVICE || c.type() == ComponentType.COMPONENT);
            case "spring/repositories.md" -> componentPage("Repositories", scan, c -> c.type() == ComponentType.REPOSITORY);
            case "spring/configuration-properties.md" -> componentPage("Configuration Properties", scan, c -> c.type() == ComponentType.CONFIGURATION_PROPERTIES || c.type() == ComponentType.CONFIGURATION);
            case "spring/validation.md" -> componentPage("Validation", scan, c -> c.type() == ComponentType.VALID || c.type() == ComponentType.VALIDATED);
            case "spring/exception-handling.md" -> componentPage("Exception Handling", scan, c -> c.type() == ComponentType.CONTROLLER_ADVICE || c.type() == ComponentType.EXCEPTION_HANDLER);
            case "data/entities.md" -> componentPage("Entities", scan, c -> c.type() == ComponentType.ENTITY);
            case "data/migrations.md" -> filePage("Migrations", scan, f -> f.category() == FileCategory.MIGRATION);
            case "data/transaction-boundaries.md" -> componentPage("Transaction Boundaries", scan, c -> c.type() == ComponentType.TRANSACTIONAL);
            case "testing/testcontainers.md" -> dependencyPage("Testcontainers", scan, d -> d == DependencyType.TESTCONTAINERS);
            case "build/dependencies.md" -> dependencies(scan);
            case "spring/dtos-and-serialization.md" -> dtoGuidance(scan);
            case "spring/time-and-deterministic-design.md" -> clockGuidance("Time and Deterministic Design");
            case "testing/deterministic-patterns.md" -> clockGuidance("Deterministic Test Patterns");
            case "data/data-consistency-rules.md" -> consistencyGuidance();
            case "agent/coding-rules.md" -> codingRules();
            case "operations/virtual-threads.md" -> virtualThreads(scan);
            case "operations/actuator.md" -> dependencyPage("Actuator", scan, d -> d == DependencyType.SPRING_ACTUATOR);
            default -> generic(file, scan);
        };
    }

    private String index(RepositoryScan scan) {
        return "# Spring Boot Agent Wiki\n\n"
                + "Generated deterministic static-analysis wiki for `" + scan.repositoryRoot() + "`.\n\n"
                + "## Quick Facts\n\n"
                + "- Scanned files: " + scan.files().size() + "\n"
                + "- Detected Spring annotations: " + scan.components().size() + "\n"
                + "- Detected major dependencies: " + scan.dependencies().size() + "\n\n"
                + "Start with `architecture/overview.md`, then `agent/coding-rules.md`.\n";
    }

    private String manifest(RepositoryScan scan) {
        return "# Wiki Manifest\n\n"
                + "- Generated at: `" + scan.generatedAt() + "`\n"
                + "- Repository path: `" + scan.repositoryRoot() + "`\n"
                + "- Generator: spring-boot-agent-wiki MVP deterministic scanner\n\n"
                + "## Files\n\n" + bullets(WIKI_FILES);
    }

    private String sourceLinks(RepositoryScan scan) {
        return "# Source Links and Staleness\n\nThis wiki is generated from static source inspection. Regenerate it after code changes.\n\n"
                + "## Scanned source files\n\n" + bullets(scan.files().stream().map(f -> f.path().toString()).toList());
    }

    private String dependencies(RepositoryScan scan) {
        StringBuilder builder = new StringBuilder("# Dependencies\n\n## Major dependency markers\n\n");
        for (DependencyType type : DependencyType.values()) {
            builder.append("- ").append(type.marker()).append(": ").append(scan.dependencyCounts().getOrDefault(type, 0L)).append('\n');
        }
        builder.append("\n## Source files\n\n").append(bullets(scan.dependencies().stream().map(d -> d.type().marker() + " in `" + d.path() + "`").toList()));
        return builder.toString();
    }

    private String componentPage(String title, RepositoryScan scan, Predicate<DetectedComponent> filter) {
        List<String> rows = scan.components().stream().filter(filter)
                .map(c -> "`" + c.className() + "` - " + c.type().annotation() + " - `" + c.path() + "`")
                .toList();
        return "# " + title + "\n\nDetected by annotation string matching.\n\n" + bulletsOrNone(rows);
    }

    private String filePage(String title, RepositoryScan scan, Predicate<ScannedFile> filter) {
        return "# " + title + "\n\n" + bulletsOrNone(scan.files().stream().filter(filter).map(f -> "`" + f.path() + "`").toList());
    }

    private String dependencyPage(String title, RepositoryScan scan, Predicate<DependencyType> filter) {
        return "# " + title + "\n\n" + bulletsOrNone(scan.dependencies().stream().filter(d -> filter.test(d.type()))
                .map(d -> d.type().marker() + " in `" + d.path() + "`").toList());
    }

    private String dtoGuidance(RepositoryScan scan) {
        return "# DTOs and Serialization\n\n"
                + "- Prefer Java records for immutable request/response DTOs when the JSON shape is explicit.\n"
                + "- Evolve records compatibly: add nullable or defaultable fields first, avoid renaming constructor components without a migration plan, and document wire-format changes.\n"
                + "- For Jackson deserialization, keep canonical record component names stable and use `@JsonProperty` when the JSON name must differ from Java naming.\n"
                + "- Avoid leaking JPA entities directly as API DTOs.\n\n"
                + filePage("Candidate DTO Sources", scan, f -> f.category() == FileCategory.MAIN_JAVA);
    }

    private String clockGuidance(String title) {
        return "# " + title + "\n\n"
                + "- Inject `java.time.Clock` instead of calling `Instant.now()` or `LocalDate.now()` directly in business logic.\n"
                + "- Use fixed clocks in tests to make time-sensitive assertions deterministic.\n"
                + "- Pass time decisions through services so scheduled jobs, retries, and expiration rules can be tested without sleeping.\n";
    }

    private String consistencyGuidance() {
        return "# Data Consistency Rules\n\n"
                + "- Model state transitions explicitly and reject invalid transitions at aggregate/service boundaries.\n"
                + "- Design commands to be idempotent where retries are possible; use natural keys, request IDs, or processed-event tables.\n"
                + "- Keep transaction scopes small and document invariants protected by each transaction.\n"
                + "- Prefer optimistic locking for concurrent aggregate updates unless pessimistic locks are justified.\n";
    }

    private String codingRules() {
        return "# Agent Coding Rules\n\n"
                + "- Preserve deterministic behavior: no hidden network calls, sleeps, random UUIDs, or wall-clock reads in domain logic without injection.\n"
                + "- Treat state-transition code as high risk; update tests for valid, invalid, repeated, and out-of-order transitions.\n"
                + "- Make retryable handlers idempotent using request IDs, unique constraints, or durable deduplication records.\n"
                + "- Do not overwrite source files when regenerating this wiki; write only under `spring-boot-agent-wiki/`.\n";
    }

    private String virtualThreads(RepositoryScan scan) {
        return "# Virtual Threads\n\n"
                + "Java 21 virtual threads can improve blocking I/O scalability, but they do not make CPU-bound work faster.\n\n"
                + "- Consider `spring.threads.virtual.enabled=true` only after validating JDBC drivers, HTTP clients, and thread-local assumptions.\n"
                + "- Avoid pinning virtual threads with long synchronized blocks or native calls.\n"
                + "- Benchmark representative request flows before and after enabling.\n\n"
                + dependencyPage("Relevant Dependencies", scan, d -> true);
    }

    private String generic(String file, RepositoryScan scan) {
        String title = Arrays.stream(file.replace(".md", "").split("/"))
                .reduce((first, second) -> second).orElse(file).replace('-', ' ');
        return "# " + Character.toUpperCase(title.charAt(0)) + title.substring(1) + "\n\n"
                + "Static-analysis notes for `" + scan.repositoryRoot() + "`.\n\n"
                + "## Detected source anchors\n\n"
                + bulletsOrNone(scan.files().stream().limit(20).map(f -> "`" + f.path() + "`").toList());
    }

    private String bullets(List<String> values) {
        return values.stream().map(value -> "- " + value).reduce("", (a, b) -> a + b + "\n");
    }

    private String bulletsOrNone(List<String> values) {
        return values.isEmpty() ? "_None detected._\n" : bullets(values);
    }
}
