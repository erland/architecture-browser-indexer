package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import java.time.Instant;
import java.util.Map;

public record HttpWorkerErrorResponse(
    String code,
    String message,
    Instant generatedAt,
    String jobId,
    Map<String, Object> details
) {
    public HttpWorkerErrorResponse {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
