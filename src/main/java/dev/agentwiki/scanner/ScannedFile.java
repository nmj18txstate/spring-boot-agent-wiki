package dev.agentwiki.scanner;

import java.nio.file.Path;

public record ScannedFile(Path path, FileCategory category) {}
