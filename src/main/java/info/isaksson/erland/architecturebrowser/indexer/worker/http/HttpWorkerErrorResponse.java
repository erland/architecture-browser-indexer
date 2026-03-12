package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record HttpWorkerErrorResponse(
    String code,
    String message,
    Instant generatedAt,
    String jobId,
    Map<String, Object> details
) {
    public HttpWorkerErrorResponse {
        details = details == null ? Map.of() : sanitize(details);
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
