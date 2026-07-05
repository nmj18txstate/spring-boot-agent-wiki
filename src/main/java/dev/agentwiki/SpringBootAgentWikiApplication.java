package dev.agentwiki;

import dev.agentwiki.markdown.MarkdownWikiGenerator;
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
        generator.generate(scanner.scan(repo), repo.resolve("spring-boot-agent-wiki"));
        System.out.println("Generated wiki at " + repo.resolve("spring-boot-agent-wiki"));
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
