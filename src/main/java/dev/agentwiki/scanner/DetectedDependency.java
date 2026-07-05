package dev.agentwiki.scanner;

import java.nio.file.Path;

public record DetectedDependency(DependencyType type, Path path) {}
