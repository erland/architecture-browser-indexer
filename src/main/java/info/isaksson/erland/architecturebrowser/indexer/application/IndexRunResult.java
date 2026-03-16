package info.isaksson.erland.architecturebrowser.indexer.application;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;

import java.nio.file.Path;
import java.util.Map;

public record IndexRunResult(
    ArchitectureIndexDocument document,
    Path acquiredRoot,
    Path outputPath,
    Map<String, Object> summary,
    boolean temporaryWorkspace
) {
}
