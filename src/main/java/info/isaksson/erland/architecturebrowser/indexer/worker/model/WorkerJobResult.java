package info.isaksson.erland.architecturebrowser.indexer.worker.model;

import java.time.Instant;
import java.util.Map;

public record WorkerJobResult(
    String jobId,
    String status,
    Instant startedAt,
    Instant finishedAt,
    String outputPath,
    Map<String, Object> summary
) {
    public WorkerJobResult {
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }
}
