package dev.agentwiki.scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryScanner {

    private static final List<String> SKIPPED_DIRECTORIES = List.of("target", "build", ".git", ".idea", "node_modules");
    private final Clock clock;

    public RepositoryScanner() {
        this(Clock.systemUTC());
    }

    public RepositoryScanner(Clock clock) {
        this.clock = clock;
    }

    public RepositoryScan scan(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<ScannedFile> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> shouldVisit(root, path))
                    .flatMap(path -> categorize(root, path).stream().map(category -> new ScannedFile(root.relativize(path), category)))
                    .sorted(Comparator.comparing(file -> file.path().toString()))
                    .toList();
        }

        List<DetectedComponent> components = files.stream()
                .filter(file -> file.category() == FileCategory.MAIN_JAVA || file.category() == FileCategory.TEST_JAVA)
                .flatMap(file -> detectComponents(root, file).stream())
                .toList();
        List<DetectedDependency> dependencies = files.stream()
                .filter(file -> file.category() == FileCategory.BUILD)
                .flatMap(file -> detectDependencies(root, file).stream())
                .toList();

        return new RepositoryScan(root, clock.instant(), files, components, dependencies,
                countComponents(components), countDependencies(dependencies));
    }

    private boolean shouldVisit(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path part : relative) {
            if (SKIPPED_DIRECTORIES.contains(part.toString())) {
                return false;
            }
        }
        return true;
    }

    private Optional<FileCategory> categorize(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String fileName = path.getFileName().toString();
        if (relative.startsWith("src/main/java/") && fileName.endsWith(".java")) return Optional.of(FileCategory.MAIN_JAVA);
        if (relative.startsWith("src/test/java/") && fileName.endsWith(".java")) return Optional.of(FileCategory.TEST_JAVA);
        if (relative.startsWith("src/main/resources/")) return Optional.of(FileCategory.MAIN_RESOURCES);
        if (relative.startsWith("db/migration/")) return Optional.of(FileCategory.MIGRATION);
        if (fileName.equals("pom.xml") || fileName.equals("build.gradle")) return Optional.of(FileCategory.BUILD);
        if (fileName.equals("Dockerfile") || fileName.equals("docker-compose.yml")) return Optional.of(FileCategory.DOCKER);
        boolean yaml = fileName.endsWith(".yaml") || fileName.endsWith(".yml");
        boolean kubernetesPath = relative.toLowerCase().contains("k8s") || relative.toLowerCase().contains("kubernetes");
        if (yaml && kubernetesPath) return Optional.of(FileCategory.KUBERNETES);
        return Optional.empty();
    }

    private List<DetectedComponent> detectComponents(Path root, ScannedFile file) {
        String content = readString(root.resolve(file.path()));
        String className = file.path().getFileName().toString().replace(".java", "");
        return Stream.of(ComponentType.values())
                .filter(type -> content.contains(type.annotation()))
                .map(type -> new DetectedComponent(type, file.path(), className))
                .toList();
    }

    private List<DetectedDependency> detectDependencies(Path root, ScannedFile file) {
        String content = readString(root.resolve(file.path())).toLowerCase();
        return Stream.of(DependencyType.values())
                .filter(type -> content.contains(type.marker().toLowerCase()))
                .map(type -> new DetectedDependency(type, file.path()))
                .toList();
    }

    private String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private Map<ComponentType, Long> countComponents(List<DetectedComponent> components) {
        return components.stream().collect(Collectors.groupingBy(DetectedComponent::type,
                () -> new EnumMap<>(ComponentType.class), Collectors.counting()));
    }

    private Map<DependencyType, Long> countDependencies(List<DetectedDependency> dependencies) {
        return dependencies.stream().collect(Collectors.groupingBy(DetectedDependency::type,
                () -> new EnumMap<>(DependencyType.class), Collectors.counting()));
    }
}
