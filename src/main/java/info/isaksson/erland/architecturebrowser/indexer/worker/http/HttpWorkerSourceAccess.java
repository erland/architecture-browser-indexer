package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import java.time.Instant;

/**
 * Describes the stable lookup key the platform should persist when it later
 * needs to request source text from the indexer.
 *
 * <p>The contract intentionally exposes a durable source handle rather than
 * worker-local file system paths. Later source-read requests should therefore be
 * keyed by {@code sourceHandle + repository-relative path}.</p>
 */
public record HttpWorkerSourceAccess(
    String lookupKeyKind,
    String sourceHandle,
    String retainedRootKind,
    String acquisitionType,
    String repositoryId,
    String sourceRevision,
    String retentionPolicy,
    Instant createdAt,
    Instant expiresAt
) {
    public static final String LOOKUP_KEY_KIND_SOURCE_HANDLE = "SOURCE_HANDLE";

    public HttpWorkerSourceAccess {
        lookupKeyKind = blankToNull(lookupKeyKind) == null ? LOOKUP_KEY_KIND_SOURCE_HANDLE : lookupKeyKind;
        sourceHandle = blankToNull(sourceHandle);
        retainedRootKind = blankToNull(retainedRootKind);
        acquisitionType = blankToNull(acquisitionType);
        repositoryId = blankToNull(repositoryId);
        sourceRevision = blankToNull(sourceRevision);
        retentionPolicy = blankToNull(retentionPolicy);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
