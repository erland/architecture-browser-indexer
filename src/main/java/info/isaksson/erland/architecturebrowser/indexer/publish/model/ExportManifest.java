package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import java.time.Instant;
import java.util.Map;

public record ExportManifest(
    String exportId,
    Instant generatedAt,
    String payloadFileName,
    String payloadContentType,
    long payloadSizeBytes,
    String payloadSha256,
    ExportContract contract,
    Map<String, Object> metadata
) {
    public ExportManifest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
