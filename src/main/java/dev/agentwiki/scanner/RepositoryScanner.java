package dev.agentwiki.scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryScanner {

    private static final List<String> SKIPPED_DIRECTORIES = List.of(
            "target",
            "build",
            ".git",
            ".idea",
            "node_modules",
            "out",
            "spring-boot-agent-wiki"
    );

    private final Clock clock;

    public RepositoryScanner() {
        this(Clock.systemUTC());
    }

    public RepositoryScanner(Clock clock) {
        this.clock = clock;
    }

    public RepositoryScan scan(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<ScannedFile> files = scanFiles(root);

        List<DetectedComponent> components = files.stream()
                .filter(file -> file.category() == FileCategory.MAIN_JAVA || file.category() == FileCategory.TEST_JAVA)
                .flatMap(file -> detectComponents(root, file).stream())
                .toList();

        List<DetectedDependency> dependencies = files.stream()
                .filter(file -> file.category() == FileCategory.BUILD)
                .flatMap(file -> detectDependencies(root, file).stream())
                .toList();

        return new RepositoryScan(
                root,
                clock.instant(),
                files,
                components,
                dependencies,
                countComponents(components),
                countDependencies(dependencies)
        );
    }

    private List<ScannedFile> scanFiles(Path root) throws IOException {
        List<ScannedFile> files = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(root) && shouldSkipDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    categorize(root, file)
                            .map(category -> new ScannedFile(root.relativize(file), category))
                            .ifPresent(files::add);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return files.stream()
                .sorted(Comparator.comparing(file -> file.path().toString()))
                .toList();
    }

    private boolean shouldSkipDirectory(Path dir) {
        Path fileName = dir.getFileName();
        return fileName != null && SKIPPED_DIRECTORIES.contains(fileName.toString());
    }

    private Optional<FileCategory> categorize(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String fileName = path.getFileName().toString();

        if (isMigrationPath(relative)) {
            return Optional.of(FileCategory.MIGRATION);
        }

        if (relative.startsWith("src/main/java/") && fileName.endsWith(".java")) {
            return Optional.of(FileCategory.MAIN_JAVA);
        }

        if (relative.startsWith("src/test/java/") && fileName.endsWith(".java")) {
            return Optional.of(FileCategory.TEST_JAVA);
        }

        if (relative.startsWith("src/main/resources/")) {
            return Optional.of(FileCategory.MAIN_RESOURCES);
        }

        if (fileName.equals("pom.xml") || fileName.equals("build.gradle") || fileName.equals("build.gradle.kts")) {
            return Optional.of(FileCategory.BUILD);
        }

        if (fileName.equals("Dockerfile") || fileName.equals("docker-compose.yml") || fileName.equals("docker-compose.yaml")) {
            return Optional.of(FileCategory.DOCKER);
        }

        boolean yaml = fileName.endsWith(".yaml") || fileName.endsWith(".yml");
        if (yaml && isKubernetesManifest(path, relative)) {
            return Optional.of(FileCategory.KUBERNETES);
        }

        return Optional.empty();
    }

    private boolean isKubernetesManifest(Path path, String relative) {
        String normalized = relative.toLowerCase();
        boolean kubernetesPath = Stream.of("k8s/", "kubernetes/", "manifests/", "helm/", "deploy/")
                .anyMatch(normalized::contains);
        if (!kubernetesPath) {
            return false;
        }

        String content = readString(path).toLowerCase();
        return Stream.of(
                        "apiversion",
                        "kind",
                        "metadata",
                        "deployment",
                        "service",
                        "configmap",
                        "secret",
                        "ingress",
                        "statefulset",
                        "daemonset",
                        "job",
                        "cronjob",
                        "horizontalpodautoscaler")
                .anyMatch(content::contains);
    }

    private boolean isMigrationPath(String relative) {
        return relative.startsWith("db/migration/")
                || relative.startsWith("db/changelog/")
                || relative.startsWith("src/main/resources/db/migration/")
                || relative.startsWith("src/main/resources/db/changelog/");
    }

    private List<DetectedComponent> detectComponents(Path root, ScannedFile file) {
        String content = readString(root.resolve(file.path()));
        String className = file.path().getFileName().toString().replace(".java", "");

        return Stream.of(ComponentType.values())
                .filter(type -> containsAnnotation(content, type.annotation()))
                .map(type -> new DetectedComponent(type, file.path(), className))
                .toList();
    }

    private boolean containsAnnotation(String content, String annotation) {
        if (content == null || content.isBlank() || annotation == null || annotation.isBlank()) {
            return false;
        }

        String annotationName = annotation.startsWith("@")
                ? annotation.substring(1)
                : annotation;

        String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);

        Pattern annotationPattern = Pattern.compile(
                "@\\s*(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)*"
                        + Pattern.quote(simpleName)
                        + "(?![A-Za-z0-9_$])"
        );

        return annotationPattern.matcher(content).find();
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
        return components.stream()
                .collect(Collectors.groupingBy(
                        DetectedComponent::type,
                        () -> new EnumMap<>(ComponentType.class),
                        Collectors.counting()
                ));
    }

    private Map<DependencyType, Long> countDependencies(List<DetectedDependency> dependencies) {
        return dependencies.stream()
                .collect(Collectors.groupingBy(
                        DetectedDependency::type,
                        () -> new EnumMap<>(DependencyType.class),
                        Collectors.counting()
                ));
    }
}