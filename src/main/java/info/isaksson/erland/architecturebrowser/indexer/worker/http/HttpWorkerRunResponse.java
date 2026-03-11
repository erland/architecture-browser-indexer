package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;

import java.time.Instant;
import java.util.Map;

public record HttpWorkerRunResponse(
    String jobId,
    String status,
    Instant startedAt,
    Instant finishedAt,
    String outputPath,
    String snapshotOut,
    ArchitectureIndexDocument document,
    Map<String, Object> manifest,
    Map<String, Object> summary
) {
    public HttpWorkerRunResponse {
        manifest = manifest == null ? Map.of() : Map.copyOf(manifest);
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }
}
