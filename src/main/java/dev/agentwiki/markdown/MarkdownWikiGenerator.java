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
import java.util.Locale;
import java.util.function.Predicate;

public class MarkdownWikiGenerator {

    public static final List<String> WIKI_FILES = List.of(
            "index.md", "wiki-manifest.md", "source-links-and-staleness.md",
            "architecture/overview.md", "architecture/request-flow.md", "architecture/package-boundaries.md", "architecture/domain-modeling.md", "architecture/dependency-map.md", "architecture/bean-dependency-map.md", "architecture/module-map.md", "architecture/sequence-diagrams.md",
            "spring/controllers.md", "spring/services.md", "spring/repositories.md", "spring/configuration-properties.md", "spring/auto-configuration-and-conditions.md", "spring/validation.md", "spring/dtos-and-serialization.md", "spring/time-and-deterministic-design.md", "spring/external-clients.md", "spring/scheduled-jobs.md", "spring/exception-handling.md", "spring/security.md", "spring/caching.md", "spring/messaging.md", "spring/events.md",
            "data/entities.md", "data/repositories.md", "data/migrations.md", "data/database-initialization.md", "data/transaction-boundaries.md", "data/query-patterns.md", "data/data-consistency-rules.md",
            "testing/unit-tests.md", "testing/integration-tests.md", "testing/test-slices.md", "testing/deterministic-patterns.md", "testing/testcontainers.md", "testing/mock-patterns.md", "testing/contract-tests.md",
            "operations/actuator.md", "operations/health-readiness-liveness.md", "operations/profiles.md", "operations/logging.md", "operations/observability.md", "operations/graceful-shutdown.md", "operations/virtual-threads.md", "operations/docker.md", "operations/kubernetes.md", "operations/native-images-and-aot.md",
            "build/dependencies.md", "build/plugins.md", "build/packaging.md", "build/java-version-and-runtime.md",
            "agent/coding-rules.md", "agent/safe-change-checklist.md", "agent/known-risk-areas.md", "agent/change-impact-analysis.md", "agent/agent-entrypoints.md");

    public void generate(RepositoryScan scan, Path wikiRoot) throws IOException {
        Files.createDirectories(wikiRoot);
        Path normalizedRoot = wikiRoot.toAbsolutePath().normalize();
        for (String wikiFile : WIKI_FILES) {
            Path output = normalizedRoot.resolve(wikiFile).normalize();
            if (!output.startsWith(normalizedRoot)) throw new IOException("Refusing to write outside wiki root: " + output);
            Files.createDirectories(output.getParent());
            Files.writeString(output, contentFor(wikiFile, scan), StandardCharsets.UTF_8);
        }
    }

    private String contentFor(String file, RepositoryScan scan) {
        return switch (file) {
            case "index.md" -> index(scan);
            case "wiki-manifest.md" -> manifest(scan);
            case "source-links-and-staleness.md" -> sourceLinks(scan);
            case "architecture/dependency-map.md" -> dependencyMap(scan);
            case "architecture/bean-dependency-map.md" -> componentPage("Bean Dependency Map", scan, c -> true) + guidance("Use constructor injection and update this page after changing bean collaborators.");
            case "architecture/module-map.md" -> moduleMap(scan);
            case "architecture/sequence-diagrams.md" -> sequenceDiagrams(scan);
            case "spring/controllers.md" -> componentPage("Controllers", scan, c -> c.type() == ComponentType.REST_CONTROLLER || c.type() == ComponentType.CONTROLLER);
            case "spring/services.md" -> componentPage("Services", scan, c -> c.type() == ComponentType.SERVICE || c.type() == ComponentType.COMPONENT);
            case "spring/repositories.md", "data/repositories.md" -> componentPage("Repositories", scan, c -> c.type() == ComponentType.REPOSITORY);
            case "spring/configuration-properties.md" -> componentPage("Configuration Properties", scan, c -> c.type() == ComponentType.CONFIGURATION_PROPERTIES || c.type() == ComponentType.CONFIGURATION);
            case "spring/auto-configuration-and-conditions.md" -> autoConfiguration(scan);
            case "spring/validation.md" -> componentPage("Validation", scan, c -> c.type() == ComponentType.VALID || c.type() == ComponentType.VALIDATED);
            case "spring/exception-handling.md" -> componentPage("Exception Handling", scan, c -> c.type() == ComponentType.CONTROLLER_ADVICE || c.type() == ComponentType.EXCEPTION_HANDLER);
            case "spring/scheduled-jobs.md" -> componentPage("Scheduled Jobs", scan, c -> c.type() == ComponentType.SCHEDULED);
            case "spring/security.md" -> markersPage("Security", scan, List.of("spring-boot-starter-security", "SecurityFilterChain", "@EnableWebSecurity"), "Review authentication, authorization, CSRF, CORS, and method-security tests before changing security code.");
            case "spring/caching.md" -> markersPage("Caching", scan, List.of("@Cacheable", "@CachePut", "@CacheEvict", "@EnableCaching"), "Document cache keys, invalidation, TTLs, and consistency risks when adding caching.");
            case "spring/messaging.md" -> markersPage("Messaging", scan, List.of("kafka", "rabbitmq", "amqp", "jms", "pulsar", "rsocket", "spring-integration"), "Document topics, queues, idempotency, retry, ordering, and dead-letter behavior when messaging is added.");
            case "spring/events.md" -> markersPage("Events", scan, List.of("ApplicationEventPublisher", "@EventListener", "@TransactionalEventListener"), "Document transaction phase, listener ordering, retries, and idempotency when events are added.");
            case "spring/external-clients.md" -> markersPage("External Clients", scan, List.of("RestClient", "WebClient", "RestTemplate", "@FeignClient", "FeignClient"), "Wrap outbound calls behind services, configure timeouts, retries, and deterministic tests.");
            case "data/entities.md" -> componentPage("Entities", scan, c -> c.type() == ComponentType.ENTITY);
            case "data/migrations.md" -> filePage("Migrations", scan, f -> f.category() == FileCategory.MIGRATION);
            case "data/database-initialization.md" -> markersPage("Database Initialization", scan, List.of("flyway", "liquibase", "schema.sql", "data.sql", "ddl-auto"), "Prefer versioned migrations; keep schema.sql/data.sql test-only unless deliberately used; avoid production ddl-auto mutations.");
            case "data/transaction-boundaries.md" -> componentPage("Transaction Boundaries", scan, c -> c.type() == ComponentType.TRANSACTIONAL);
            case "data/query-patterns.md" -> markersPage("Query Patterns", scan, List.of("@Query", "nativeQuery", "findBy", "existsBy", "deleteBy", "countBy"), "For query changes, check indexes, pagination, transaction boundaries, and N+1 risks.");
            case "testing/unit-tests.md" -> testPage("Unit Tests", scan, List.of("Test.java"), "Keep unit tests fast, isolated, and deterministic.");
            case "testing/integration-tests.md" -> testPage("Integration Tests", scan, List.of("IT.java", "IntegrationTest.java", "@SpringBootTest"), "Use integration tests for wiring, serialization, persistence, and security boundaries.");
            case "testing/mock-patterns.md" -> markersPage("Mock Patterns", scan, List.of("Mockito", "@Mock", "@MockBean", "@MockitoBean"), "Mock at external seams; prefer real domain objects for business rules.");
            case "testing/contract-tests.md" -> markersPage("Contract Tests", scan, List.of("spring-cloud-contract", "pact", "wiremock", "MockWebServer"), "Add consumer/provider contract coverage before changing public API shapes.");
            case "testing/testcontainers.md" -> dependencyPage("Testcontainers", scan, d -> d == DependencyType.TESTCONTAINERS);
            case "operations/docker.md" -> filePage("Docker", scan, f -> f.category() == FileCategory.DOCKER) + guidance("If Docker is added, document base image, exposed ports, health checks, layered jars, and non-root runtime.");
            case "operations/kubernetes.md" -> filePage("Kubernetes", scan, f -> f.category() == FileCategory.KUBERNETES) + guidance("If Kubernetes is added, document Deployments, Services, ConfigMaps, Secrets, probes, resources, and rollout strategy.");
            case "operations/native-images-and-aot.md" -> nativeImages();
            case "operations/virtual-threads.md" -> virtualThreads(scan);
            case "operations/actuator.md" -> dependencyPage("Actuator", scan, d -> d == DependencyType.SPRING_ACTUATOR);
            case "build/dependencies.md" -> dependencies(scan);
            case "build/plugins.md" -> markersPage("Build Plugins", scan, List.of("spring-boot-maven-plugin", "maven-compiler-plugin", "maven-surefire-plugin", "maven-failsafe-plugin", "org.springframework.boot", "com.google.cloud.tools.jib", "graalvm"), "Keep plugin versions explicit and aligned with Java 21/Spring Boot compatibility.");
            case "build/packaging.md" -> packaging(scan);
            case "spring/dtos-and-serialization.md" -> dtoGuidance(scan);
            case "spring/time-and-deterministic-design.md" -> clockGuidance("Time and Deterministic Design");
            case "testing/deterministic-patterns.md" -> clockGuidance("Deterministic Test Patterns");
            case "data/data-consistency-rules.md" -> consistencyGuidance();
            case "agent/coding-rules.md" -> codingRules();
            case "agent/change-impact-analysis.md" -> changeImpact();
            case "agent/agent-entrypoints.md" -> agentEntrypoints();
            default -> generic(file, scan);
        };
    }
    private String index(RepositoryScan scan) { return "# Spring Boot Agent Wiki\n\nGenerated deterministic static-analysis wiki for `" + scan.repositoryRoot() + "`.\n\n## Quick Facts\n\n- Scanned files: " + scan.files().size() + "\n- Detected Spring annotations: " + scan.components().size() + "\n- Detected major dependencies: " + scan.dependencies().size() + "\n\nStart with `architecture/overview.md`, then `agent/agent-entrypoints.md` and `agent/coding-rules.md`.\n"; }
    private String manifest(RepositoryScan scan) { return "# Wiki Manifest\n\n- Generated at: `" + scan.generatedAt() + "`\n- Repository path: `" + scan.repositoryRoot() + "`\n- Generator: spring-boot-agent-wiki Phase 1.1 deterministic scanner\n\n## Files\n\n" + bullets(WIKI_FILES); }
    private String sourceLinks(RepositoryScan scan) { return "# Source Links and Staleness\n\nThis wiki is generated from static source inspection. Regenerate it after code changes. The tool writes only under `spring-boot-agent-wiki/`.\n\n## Scanned source files\n\n" + bulletsOrNone(scan.files().stream().map(f -> "`" + f.path() + "` (" + f.category() + ")").toList()); }
    private String dependencies(RepositoryScan scan) { StringBuilder b = new StringBuilder("# Dependencies\n\n## Major dependency markers\n\n"); for (DependencyType type : DependencyType.values()) b.append("- ").append(type.marker()).append(": ").append(scan.dependencyCounts().getOrDefault(type, 0L)).append('\n'); return b.append("\n## Source files\n\n").append(bulletsOrNone(scan.dependencies().stream().map(d -> d.type().marker() + " in `" + d.path() + "`").toList())).toString(); }
    private String dependencyMap(RepositoryScan scan) { return "# Dependency Map\n\n## Component inventory\n\n" + componentPage("Detected Components", scan, c -> true) + "\n## Build dependency markers\n\n" + dependencies(scan) + guidance("Current dependency mapping is static marker based. Before changing a class, inspect its constructor parameters, injected fields, and callers."); }
    private String moduleMap(RepositoryScan scan) { long builds = scan.files().stream().filter(f -> f.category() == FileCategory.BUILD).count(); String mode = builds > 1 ? "Multi-module indicators detected from multiple build files." : "Single-module indicators detected or only one build file found."; return "# Module Map\n\n" + mode + "\n\n## Build files\n\n" + bulletsOrNone(scan.files().stream().filter(f -> f.category() == FileCategory.BUILD).map(f -> "`" + f.path() + "`").toList()) + guidance("For multi-module changes, identify API, implementation, test-fixture, and application module boundaries before editing."); }
    private String sequenceDiagrams(RepositoryScan scan) { return "# Sequence Diagrams\n\nStatic scanner generated template flows. Replace names with concrete classes when making changes.\n\n```mermaid\nsequenceDiagram\n    actor Client\n    participant Controller\n    participant Service\n    participant Repository\n    participant Database\n    Client->>Controller: HTTP request\n    Controller->>Service: validate and delegate\n    Service->>Repository: load/save aggregate\n    Repository->>Database: query/update\n    Database-->>Repository: result\n    Repository-->>Service: entity/data\n    Service-->>Controller: DTO/result\n    Controller-->>Client: HTTP response\n```\n\n## Detected controllers/services/repositories\n\n" + componentPage("Flow Anchors", scan, c -> c.type()==ComponentType.REST_CONTROLLER||c.type()==ComponentType.CONTROLLER||c.type()==ComponentType.SERVICE||c.type()==ComponentType.REPOSITORY); }
    private String autoConfiguration(RepositoryScan scan) { return "# Auto-configuration and Conditions\n\nDetected starters imply likely Spring Boot auto-configuration areas.\n\n" + dependencies(scan) + guidance("When adding starters, review generated conditions, default beans, properties, and test slices. Override auto-config only with explicit, tested configuration."); }
    private String componentPage(String title, RepositoryScan scan, Predicate<DetectedComponent> filter) { List<String> rows = scan.components().stream().filter(filter).map(c -> "`" + c.className() + "` - " + c.type().annotation() + " - `" + c.path() + "`").toList(); return "# " + title + "\n\nDetected by deterministic annotation string matching.\n\n" + bulletsOrNone(rows); }
    private String filePage(String title, RepositoryScan scan, Predicate<ScannedFile> filter) { return "# " + title + "\n\n" + bulletsOrNone(scan.files().stream().filter(filter).map(f -> "`" + f.path() + "`").toList()); }
    private String dependencyPage(String title, RepositoryScan scan, Predicate<DependencyType> filter) { return "# " + title + "\n\n" + bulletsOrNone(scan.dependencies().stream().filter(d -> filter.test(d.type())).map(d -> d.type().marker() + " in `" + d.path() + "`").toList()); }
    private String markersPage(String title, RepositoryScan scan, List<String> markers, String guidance) { List<String> hits = scan.files().stream().filter(f -> containsAny(scan.repositoryRoot().resolve(f.path()), markers)).map(f -> "`" + f.path() + "`").toList(); return "# " + title + "\n\nMarkers: " + String.join(", ", markers) + "\n\n" + bulletsOrNone(hits) + guidance(guidance); }
    private String testPage(String title, RepositoryScan scan, List<String> markers, String guidance) { List<String> tests = scan.files().stream().filter(f -> f.category()==FileCategory.TEST_JAVA).filter(f -> markers.stream().anyMatch(m -> f.path().toString().contains(m)) || containsAny(scan.repositoryRoot().resolve(f.path()), markers)).map(f -> "`" + f.path() + "`").toList(); return "# " + title + "\n\n" + bulletsOrNone(tests) + guidance(guidance); }
    private boolean containsAny(Path path, List<String> markers) { try { String c = Files.readString(path, StandardCharsets.UTF_8); String lower = c.toLowerCase(Locale.ROOT); return markers.stream().anyMatch(m -> lower.contains(m.toLowerCase(Locale.ROOT))); } catch (IOException e) { return false; } }
    private String dtoGuidance(RepositoryScan scan) { return "# DTOs and Serialization\n\n- Prefer Java records for immutable request/response DTOs when the JSON shape is explicit.\n- Evolve records compatibly and document wire-format changes.\n- For Jackson deserialization, keep canonical record component names stable and use `@JsonProperty` when needed.\n- Avoid leaking JPA entities directly as API DTOs.\n\n" + filePage("Candidate DTO Sources", scan, f -> f.category() == FileCategory.MAIN_JAVA); }
    private String clockGuidance(String title) { return "# " + title + "\n\n- Inject `java.time.Clock` instead of calling `Instant.now()` or `LocalDate.now()` directly in business logic.\n- Use fixed clocks in tests to make time-sensitive assertions deterministic.\n- Pass time decisions through services so scheduled jobs, retries, and expiration rules can be tested without sleeping.\n"; }
    private String consistencyGuidance() { return "# Data Consistency Rules\n\n- Model state transitions explicitly and reject invalid transitions at aggregate/service boundaries.\n- Design commands to be idempotent where retries are possible.\n- Keep transaction scopes small and document invariants protected by each transaction.\n- Prefer optimistic locking for concurrent aggregate updates unless pessimistic locks are justified.\n"; }
    private String codingRules() { return "# Agent Coding Rules\n\n- Preserve deterministic behavior: no hidden network calls, sleeps, random UUIDs, or wall-clock reads in domain logic without injection.\n- Treat state-transition code as high risk; update tests for valid, invalid, repeated, and out-of-order transitions.\n- Make retryable handlers idempotent using request IDs, unique constraints, or durable deduplication records.\n- Do not overwrite source files when regenerating this wiki; write only under `spring-boot-agent-wiki/`.\n"; }
    private String changeImpact() { return "# Change Impact Analysis\n\nAgents should evaluate changed files by layer:\n\n- API/controller: review DTOs, validation, serialization, security, and contract tests.\n- Service/domain: review transaction boundaries, idempotency, events, time usage, and unit tests.\n- Data: review entities, repositories, migrations, query patterns, indexes, and rollback safety.\n- Configuration/build: review properties, profiles, auto-configuration, runtime Java version, and packaging.\n- Operations: review actuator, logging, observability, Docker, Kubernetes, AOT/native-image constraints.\n"; }
    private String agentEntrypoints() { return "# Agent Entrypoints\n\nRead these pages first by change type:\n\n- API changes: `spring/controllers.md`, `spring/dtos-and-serialization.md`, `spring/validation.md`, `spring/security.md`.\n- Service changes: `spring/services.md`, `architecture/request-flow.md`, `data/transaction-boundaries.md`.\n- DB changes: `data/entities.md`, `data/repositories.md`, `data/migrations.md`, `data/query-patterns.md`.\n- Config changes: `spring/configuration-properties.md`, `operations/profiles.md`, `spring/auto-configuration-and-conditions.md`.\n- Testing changes: `testing/unit-tests.md`, `testing/integration-tests.md`, `testing/test-slices.md`, `testing/mock-patterns.md`.\n- Operations changes: `operations/actuator.md`, `operations/docker.md`, `operations/kubernetes.md`, `operations/native-images-and-aot.md`.\n- Build changes: `build/dependencies.md`, `build/plugins.md`, `build/packaging.md`, `build/java-version-and-runtime.md`.\n"; }
    private String packaging(RepositoryScan scan) { return "# Packaging\n\n" + filePage("Build Files", scan, f -> f.category()==FileCategory.BUILD) + guidance("Document executable JAR settings, layered JARs, Docker image creation, buildpacks, and native image packaging when enabled."); }
    private String nativeImages() { return "# Native Images and AOT\n\nNo matching feature detected in this scan unless build markers above mention GraalVM or AOT.\n\nGuidance for future native-image work:\n\n- Validate GraalVM and Spring AOT compatibility for every dependency.\n- Add `RuntimeHints` for reflection, proxies, serialization, and resources.\n- Test startup and critical request flows from the native binary.\n- Avoid dynamic classpath scanning assumptions that are not visible to AOT analysis.\n"; }
    private String virtualThreads(RepositoryScan scan) { return "# Virtual Threads\n\nJava 21 virtual threads can improve blocking I/O scalability, but they do not make CPU-bound work faster.\n\n- Consider `spring.threads.virtual.enabled=true` only after validating JDBC drivers, HTTP clients, and thread-local assumptions.\n- Avoid pinning virtual threads with long synchronized blocks or native calls.\n- Benchmark representative request flows before and after enabling.\n\n" + dependencyPage("Relevant Dependencies", scan, d -> true); }
    private String generic(String file, RepositoryScan scan) { String title = Arrays.stream(file.replace(".md", "").split("/")).reduce((first, second) -> second).orElse(file).replace('-', ' '); return "# " + Character.toUpperCase(title.charAt(0)) + title.substring(1) + "\n\nStatic-analysis notes for `" + scan.repositoryRoot() + "`.\n\n## Detected source anchors\n\n" + bulletsOrNone(scan.files().stream().limit(20).map(f -> "`" + f.path() + "`").toList()) + guidance("No matching feature detected in this scan. If this area is added later, document ownership, configuration, tests, and operational risks here."); }
    private String guidance(String text) { return "\n## Agent Guidance\n\n" + (text.contains("No matching feature detected") ? "" : "No matching feature detected in this scan when the list above is empty. ") + text + "\n"; }
    private String bullets(List<String> values) { return values.stream().map(value -> "- " + value).reduce("", (a, b) -> a + b + "\n"); }
    private String bulletsOrNone(List<String> values) { return values.isEmpty() ? "No matching feature detected in this scan.\n" : bullets(values); }
}
