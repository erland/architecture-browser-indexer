package info.isaksson.erland.architecturebrowser.indexer.worker.model;

import java.time.Instant;
import java.util.LinkedHashMap;
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
