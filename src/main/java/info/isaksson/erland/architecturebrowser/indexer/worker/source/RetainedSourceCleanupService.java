package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetainedSourceCleanupService {
    private static final Logger LOG = Logger.getLogger(RetainedSourceCleanupService.class.getName());

    private final RetainedSourceHandleRegistryService registryService;

    public RetainedSourceCleanupService(RetainedSourceHandleRegistryService registryService) {
        if (registryService == null) {
            throw new IllegalArgumentException("registryService is required");
        }
        this.registryService = registryService;
    }

    public RetainedSourceCleanupReport pruneExpiredAndInvalid() {
        return pruneExpiredAndInvalid(Instant.now());
    }

    public RetainedSourceCleanupReport pruneExpiredAndInvalid(Instant now) {
        Instant referenceTime = now == null ? Instant.now() : now;
        int prunedExpiredHandles = 0;
        int prunedMissingRootHandles = 0;
        int failedDeletes = 0;
        List<RetainedSourceHandleRecord> records = registryService.list();
        for (RetainedSourceHandleRecord record : records) {
            boolean shouldDeleteAsExpired = registryService.isExpired(record, referenceTime);
            boolean shouldDeleteAsMissingRoot = !shouldDeleteAsExpired && !Files.isDirectory(record.retainedRoot());
            if (!shouldDeleteAsExpired && !shouldDeleteAsMissingRoot) {
                continue;
            }
            try {
                boolean deleted = registryService.delete(record.sourceHandle());
                if (!deleted) {
                    failedDeletes++;
                    LOG.warning(() -> "Retained-source cleanup could not delete sourceHandle=" + record.sourceHandle());
                    continue;
                }
                if (shouldDeleteAsExpired) {
                    prunedExpiredHandles++;
                } else {
                    prunedMissingRootHandles++;
                }
            } catch (RuntimeException exception) {
                failedDeletes++;
                LOG.log(Level.WARNING,
                    "Retained-source cleanup failed for sourceHandle=" + record.sourceHandle(),
                    exception);
            }
        }
        return new RetainedSourceCleanupReport(referenceTime, prunedExpiredHandles, prunedMissingRootHandles, failedDeletes);
    }
}
