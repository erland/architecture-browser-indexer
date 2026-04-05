package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.time.Instant;

public record RetainedSourceCleanupReport(
    Instant cleanedAt,
    int prunedExpiredHandles,
    int prunedMissingRootHandles,
    int failedDeletes
) {
    public RetainedSourceCleanupReport {
        cleanedAt = cleanedAt == null ? Instant.now() : cleanedAt;
        if (prunedExpiredHandles < 0 || prunedMissingRootHandles < 0 || failedDeletes < 0) {
            throw new IllegalArgumentException("Cleanup counters must be >= 0");
        }
    }

    public static RetainedSourceCleanupReport empty(Instant cleanedAt) {
        return new RetainedSourceCleanupReport(cleanedAt, 0, 0, 0);
    }

    public boolean hasChanges() {
        return prunedExpiredHandles > 0 || prunedMissingRootHandles > 0 || failedDeletes > 0;
    }
}
