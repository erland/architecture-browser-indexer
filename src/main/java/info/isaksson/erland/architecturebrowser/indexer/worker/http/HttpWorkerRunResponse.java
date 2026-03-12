package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;

import java.time.Instant;
import java.util.LinkedHashMap;
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
        manifest = manifest == null ? Map.of() : sanitize(manifest);
        summary = summary == null ? Map.of() : sanitize(summary);
    }

    private static Map<String, Object> sanitize(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            sanitized.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(sanitized);
    }
}
