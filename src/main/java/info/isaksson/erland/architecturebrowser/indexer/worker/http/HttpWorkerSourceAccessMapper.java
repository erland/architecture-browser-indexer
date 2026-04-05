package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import java.time.Instant;
import java.util.Map;

final class HttpWorkerSourceAccessMapper {
    private HttpWorkerSourceAccessMapper() {
    }

    static HttpWorkerSourceAccess fromSummary(Map<String, Object> summary) {
        if (summary == null || !(summary.get("sourceAccess") instanceof Map<?, ?> rawMap)) {
            return null;
        }
        return new HttpWorkerSourceAccess(
            stringValue(rawMap.get("lookupKeyKind")),
            stringValue(rawMap.get("sourceHandle")),
            stringValue(rawMap.get("retainedRootKind")),
            stringValue(rawMap.get("acquisitionType")),
            stringValue(rawMap.get("repositoryId")),
            stringValue(rawMap.get("sourceRevision")),
            stringValue(rawMap.get("retentionPolicy")),
            instantValue(rawMap.get("createdAt")),
            instantValue(rawMap.get("expiresAt"))
        );
    }

    private static String stringValue(Object value) {
        return value instanceof String string && !string.isBlank() ? string : null;
    }

    private static Instant instantValue(Object value) {
        String string = stringValue(value);
        return string == null ? null : Instant.parse(string);
    }
}
