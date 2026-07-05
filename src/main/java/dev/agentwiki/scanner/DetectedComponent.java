package dev.agentwiki.scanner;

import java.nio.file.Path;

public record DetectedComponent(ComponentType type, Path path, String className) {}
