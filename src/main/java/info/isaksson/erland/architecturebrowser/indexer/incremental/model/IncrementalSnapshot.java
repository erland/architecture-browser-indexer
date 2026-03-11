package info.isaksson.erland.architecturebrowser.indexer.incremental.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IncrementalSnapshot(
    String schemaVersion,
    Instant createdAt,
    Map<String, FileFingerprint> filesByPath,
    Map<String, Object> metadata
) {
    public IncrementalSnapshot {
        filesByPath = filesByPath == null ? Map.of() : Map.copyOf(filesByPath);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
