package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record RetainedSourceHandleRecord(
    String sourceHandle,
    Path retainedRoot,
    String retainedRootKind,
    String acquisitionType,
    String repositoryId,
    String gitUrl,
    String gitRef,
    String sourceRevision,
    Instant createdAt,
    Instant expiresAt,
    Instant lastAccessedAt,
    String retentionPolicy
) {
    public RetainedSourceHandleRecord {
        sourceHandle = blankToNull(sourceHandle);
        retainedRootKind = blankToNull(retainedRootKind);
        acquisitionType = blankToNull(acquisitionType);
        repositoryId = blankToNull(repositoryId);
        gitUrl = blankToNull(gitUrl);
        gitRef = blankToNull(gitRef);
        sourceRevision = blankToNull(sourceRevision);
        retentionPolicy = blankToNull(retentionPolicy);
        if (retainedRoot != null) {
            retainedRoot = retainedRoot.toAbsolutePath().normalize();
        }
    }

    public Map<String, Object> toSummaryMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        putIfNotNull(result, "lookupKeyKind", "SOURCE_HANDLE");
        putIfNotNull(result, "sourceHandle", sourceHandle);
        putIfNotNull(result, "retainedRootKind", retainedRootKind);
        putIfNotNull(result, "acquisitionType", acquisitionType);
        putIfNotNull(result, "repositoryId", repositoryId);
        putIfNotNull(result, "sourceRevision", sourceRevision);
        putIfNotNull(result, "retentionPolicy", retentionPolicy);
        putIfNotNull(result, "createdAt", createdAt == null ? null : createdAt.toString());
        putIfNotNull(result, "expiresAt", expiresAt == null ? null : expiresAt.toString());
        return Map.copyOf(result);
    }

    public Map<String, Object> toRegistryMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        putIfNotNull(result, "sourceHandle", sourceHandle);
        putIfNotNull(result, "retainedRoot", retainedRoot == null ? null : retainedRoot.toString());
        putIfNotNull(result, "retainedRootKind", retainedRootKind);
        putIfNotNull(result, "acquisitionType", acquisitionType);
        putIfNotNull(result, "repositoryId", repositoryId);
        putIfNotNull(result, "gitUrl", gitUrl);
        putIfNotNull(result, "gitRef", gitRef);
        putIfNotNull(result, "sourceRevision", sourceRevision);
        putIfNotNull(result, "createdAt", createdAt == null ? null : createdAt.toString());
        putIfNotNull(result, "expiresAt", expiresAt == null ? null : expiresAt.toString());
        putIfNotNull(result, "lastAccessedAt", lastAccessedAt == null ? null : lastAccessedAt.toString());
        putIfNotNull(result, "retentionPolicy", retentionPolicy);
        return Map.copyOf(result);
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (key != null && value != null) {
            target.put(key, value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
