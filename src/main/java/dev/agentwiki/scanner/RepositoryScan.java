package dev.agentwiki.scanner;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RepositoryScan(
        Path repositoryRoot,
        Instant generatedAt,
        List<ScannedFile> files,
        List<DetectedComponent> components,
        List<DetectedDependency> dependencies,
        Map<ComponentType, Long> componentCounts,
        Map<DependencyType, Long> dependencyCounts) {}
