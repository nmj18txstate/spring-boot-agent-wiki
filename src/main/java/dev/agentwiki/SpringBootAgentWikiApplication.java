package dev.agentwiki;

import dev.agentwiki.markdown.MarkdownWikiGenerator;
import dev.agentwiki.scanner.ComponentType;
import dev.agentwiki.scanner.RepositoryScan;
import dev.agentwiki.scanner.RepositoryScanner;
import java.nio.file.Path;

public class SpringBootAgentWikiApplication {

    private final RepositoryScanner scanner;
    private final MarkdownWikiGenerator generator;

    public SpringBootAgentWikiApplication() {
        this(new RepositoryScanner(), new MarkdownWikiGenerator());
    }

    public SpringBootAgentWikiApplication(RepositoryScanner scanner, MarkdownWikiGenerator generator) {
        this.scanner = scanner;
        this.generator = generator;
    }

    public static void main(String[] args) throws Exception {
        new SpringBootAgentWikiApplication().run(args);
    }

    public void run(String... args) throws Exception {
        Path repo = parseRepoPath(args).toAbsolutePath().normalize();
        Path wikiRoot = repo.resolve("spring-boot-agent-wiki");

        System.out.println("[INFO] Scanning repository target: " + repo);
        System.out.println("[INFO] Pruning skipped directories: " + RepositoryScanner.SKIPPED_DIRECTORIES);
        System.out.println("[INFO] Categorizing project files and processing annotation boundaries...");

        RepositoryScan scan = scanner.scan(repo);
        System.out.println("[INFO] Component scan complete: Found "
                + controllerCount(scan) + " Controllers, "
                + serviceCount(scan) + " Services, "
                + repositoryCount(scan) + " Repositories.");
        System.out.println("[INFO] Generating deterministic markdown structures...");

        int pageCount = generator.generate(scan, wikiRoot);
        System.out.println("[SUCCESS] Generated " + pageCount + " agent-ready wiki pages under spring-boot-agent-wiki/");
    }

    public static long controllerCount(RepositoryScan scan) {
        return count(scan, ComponentType.REST_CONTROLLER) + count(scan, ComponentType.CONTROLLER);
    }

    public static long serviceCount(RepositoryScan scan) {
        return count(scan, ComponentType.SERVICE);
    }

    public static long repositoryCount(RepositoryScan scan) {
        return count(scan, ComponentType.REPOSITORY);
    }

    private static long count(RepositoryScan scan, ComponentType type) {
        return scan.componentCounts().getOrDefault(type, 0L);
    }

    private Path parseRepoPath(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--repo=")) {
                String value = arg.substring("--repo=".length()).trim();
                if (!value.isBlank()) {
                    return Path.of(value);
                }
            }
        }
        return Path.of("");
    }
}
