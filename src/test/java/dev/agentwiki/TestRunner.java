package dev.agentwiki;

import dev.agentwiki.markdown.MarkdownWikiGenerator;
import dev.agentwiki.scanner.ComponentType;
import dev.agentwiki.scanner.DependencyType;
import dev.agentwiki.scanner.FileCategory;
import dev.agentwiki.scanner.RepositoryScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public class TestRunner {
    public static void main(String[] args) throws Exception {
        scansSpringSourceFilesAndBuildDependencies();
        classifiesSpringBootResourceMigrationsAsMigrations();
        avoidsAnnotationPrefixFalsePositives();
        skipsGeneratedAndToolDirectories();
        generatesExpectedMarkdownFilesInsideWikiDirectory();
        writesGuidanceRequiredForMvp();
        System.out.println("All tests passed");
    }

    private static void scansSpringSourceFilesAndBuildDependencies() throws Exception {
        Path tempDir = Files.createTempDirectory("scanner-test");
        Files.createDirectories(tempDir.resolve("src/main/java/example"));
        Files.writeString(
                tempDir.resolve("src/main/java/example/GreetingController.java"),
                "package example; @RestController class GreetingController { @Transactional void handle() {} }"
        );

        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.writeString(tempDir.resolve("src/main/resources/application.yml"), "spring: {}\n");
        Files.writeString(
                tempDir.resolve("pom.xml"),
                "<artifactId>spring-boot-starter-web</artifactId><artifactId>postgresql</artifactId>"
        );

        var scan = new RepositoryScanner(fixedClock()).scan(tempDir);

        require(scan.files().stream().anyMatch(file -> file.category() == FileCategory.MAIN_JAVA), "main java scanned");
        require(scan.components().stream().anyMatch(component -> component.type() == ComponentType.REST_CONTROLLER), "controller detected");
        require(scan.components().stream().anyMatch(component -> component.type() == ComponentType.TRANSACTIONAL), "transactional detected");
        require(scan.dependencies().stream().anyMatch(dependency -> dependency.type() == DependencyType.SPRING_WEB), "web detected");
        require(scan.dependencies().stream().anyMatch(dependency -> dependency.type() == DependencyType.POSTGRESQL), "postgres detected");
    }

    private static void classifiesSpringBootResourceMigrationsAsMigrations() throws Exception {
        Path tempDir = Files.createTempDirectory("migration-test");

        Files.createDirectories(tempDir.resolve("src/main/resources/db/migration"));
        Files.writeString(tempDir.resolve("src/main/resources/db/migration/V1__init.sql"), "create table example(id bigint);\n");

        Files.createDirectories(tempDir.resolve("src/main/resources/db/changelog"));
        Files.writeString(tempDir.resolve("src/main/resources/db/changelog/db.changelog-master.yaml"), "databaseChangeLog: []\n");

        Files.createDirectories(tempDir.resolve("db/migration"));
        Files.writeString(tempDir.resolve("db/migration/V2__legacy.sql"), "alter table example add column name text;\n");

        var scan = new RepositoryScanner(fixedClock()).scan(tempDir);

        require(
                scan.files().stream().anyMatch(file ->
                        file.category() == FileCategory.MIGRATION
                                && file.path().toString().replace('\\', '/').equals("src/main/resources/db/migration/V1__init.sql")
                ),
                "Spring Boot resource Flyway migration classified as migration"
        );

        require(
                scan.files().stream().anyMatch(file ->
                        file.category() == FileCategory.MIGRATION
                                && file.path().toString().replace('\\', '/').equals("src/main/resources/db/changelog/db.changelog-master.yaml")
                ),
                "Spring Boot resource Liquibase changelog classified as migration"
        );

        require(
                scan.files().stream().anyMatch(file ->
                        file.category() == FileCategory.MIGRATION
                                && file.path().toString().replace('\\', '/').equals("db/migration/V2__legacy.sql")
                ),
                "root Flyway migration classified as migration"
        );

        require(
                scan.files().stream().noneMatch(file ->
                        file.category() == FileCategory.MAIN_RESOURCES
                                && file.path().toString().replace('\\', '/').contains("db/migration")
                ),
                "migration files are not classified as generic resources"
        );
    }

    private static void avoidsAnnotationPrefixFalsePositives() throws Exception {
        Path tempDir = Files.createTempDirectory("annotation-boundary-test");
        Files.createDirectories(tempDir.resolve("src/main/java/example"));

        Files.writeString(
                tempDir.resolve("src/main/java/example/GlobalExceptionHandler.java"),
                "package example; @ControllerAdvice class GlobalExceptionHandler {}"
        );

        Files.writeString(
                tempDir.resolve("src/main/java/example/AppProperties.java"),
                "package example; @ConfigurationProperties(prefix = \"app\") record AppProperties(String name) {}"
        );

        Files.writeString(
                tempDir.resolve("src/main/java/example/InputValidator.java"),
                "package example; @Validated class InputValidator {}"
        );

        var scan = new RepositoryScanner(fixedClock()).scan(tempDir);

        require(
                scan.components().stream().anyMatch(component -> component.type() == ComponentType.CONTROLLER_ADVICE),
                "@ControllerAdvice detected"
        );

        require(
                scan.components().stream().noneMatch(component -> component.type() == ComponentType.CONTROLLER),
                "@ControllerAdvice does not falsely match @Controller"
        );

        require(
                scan.components().stream().anyMatch(component -> component.type() == ComponentType.CONFIGURATION_PROPERTIES),
                "@ConfigurationProperties detected"
        );

        require(
                scan.components().stream().noneMatch(component -> component.type() == ComponentType.CONFIGURATION),
                "@ConfigurationProperties does not falsely match @Configuration"
        );

        require(
                scan.components().stream().anyMatch(component -> component.type() == ComponentType.VALIDATED),
                "@Validated detected"
        );

        require(
                scan.components().stream().noneMatch(component -> component.type() == ComponentType.VALID),
                "@Validated does not falsely match @Valid"
        );
    }

    private static void skipsGeneratedAndToolDirectories() throws Exception {
        Path tempDir = Files.createTempDirectory("skip-test");

        Files.createDirectories(tempDir.resolve("target/classes/example"));
        Files.writeString(tempDir.resolve("target/classes/example/Generated.java"), "@Service class Generated {}");

        Files.createDirectories(tempDir.resolve("build/classes/example"));
        Files.writeString(tempDir.resolve("build/classes/example/BuildGenerated.java"), "@Service class BuildGenerated {}");

        Files.createDirectories(tempDir.resolve(".git"));
        Files.writeString(tempDir.resolve(".git/config"), "spring-boot-starter-security");

        Files.createDirectories(tempDir.resolve(".idea"));
        Files.writeString(tempDir.resolve(".idea/workspace.xml"), "@Repository class IdeaGenerated {}");

        Files.createDirectories(tempDir.resolve("node_modules/example"));
        Files.writeString(tempDir.resolve("node_modules/example/Generated.java"), "@Service class NodeGenerated {}");

        Files.createDirectories(tempDir.resolve("out/example"));
        Files.writeString(tempDir.resolve("out/example/Generated.java"), "@Service class OutGenerated {}");

        Files.createDirectories(tempDir.resolve("spring-boot-agent-wiki/spring"));
        Files.writeString(tempDir.resolve("spring-boot-agent-wiki/spring/services.md"), "@Service class WikiGenerated {}");

        var scan = new RepositoryScanner(fixedClock()).scan(tempDir);

        require(scan.files().isEmpty(), "skipped directories ignored");
        require(scan.components().isEmpty(), "components in skipped directories ignored");
        require(scan.dependencies().isEmpty(), "dependencies in skipped directories ignored");
    }

    private static void generatesExpectedMarkdownFilesInsideWikiDirectory() throws Exception {
        Path repo = Files.createTempDirectory("wiki-test");
        Files.createDirectories(repo.resolve("src/main/java/example"));
        Files.writeString(repo.resolve("src/main/java/example/OrderService.java"), "@Service class OrderService {}");
        Files.writeString(repo.resolve("pom.xml"), "<artifactId>spring-boot-starter-validation</artifactId>");

        Path wikiRoot = repo.resolve("spring-boot-agent-wiki");
        var scan = new RepositoryScanner(fixedClock()).scan(repo);
        new MarkdownWikiGenerator().generate(scan, wikiRoot);

        for (String file : List.of("index.md", "wiki-manifest.md", "architecture/overview.md", "agent/coding-rules.md")) {
            require(Files.exists(wikiRoot.resolve(file)), file + " generated");
        }

        require(Files.readString(wikiRoot.resolve("spring/services.md")).contains("OrderService"), "service page populated");
    }

    private static void writesGuidanceRequiredForMvp() throws Exception {
        Path repo = Files.createTempDirectory("guidance-test");
        var scan = new RepositoryScanner(fixedClock()).scan(repo);
        Path wikiRoot = repo.resolve("spring-boot-agent-wiki");

        new MarkdownWikiGenerator().generate(scan, wikiRoot);

        require(Files.readString(wikiRoot.resolve("data/data-consistency-rules.md")).contains("idempotent"), "idempotency guidance");
        require(Files.readString(wikiRoot.resolve("spring/dtos-and-serialization.md")).contains("Jackson deserialization"), "Jackson guidance");
        require(Files.readString(wikiRoot.resolve("spring/time-and-deterministic-design.md")).contains("java.time.Clock"), "Clock guidance");
        require(Files.readString(wikiRoot.resolve("operations/virtual-threads.md")).contains("Java 21 virtual threads"), "virtual threads guidance");
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}