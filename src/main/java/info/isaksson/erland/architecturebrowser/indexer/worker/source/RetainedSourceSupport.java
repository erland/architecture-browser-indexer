package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class RetainedSourceSupport {
    public static final String RETAINED_ROOT_KIND_LOCAL_PATH_REFERENCE = "LOCAL_PATH_REFERENCE";
    public static final String RETAINED_ROOT_KIND_RETAINED_GIT_CHECKOUT = "RETAINED_GIT_CHECKOUT";
    public static final String ACQUISITION_TYPE_LOCAL_PATH = "LOCAL_PATH";
    public static final String ACQUISITION_TYPE_GIT = "GIT";
    public static final String RETENTION_POLICY_LOCAL_PATH_REFERENCE = "local-path-reference";
    public static final String RETENTION_POLICY_TTL_7D = "ttl-7d";
    public static final Duration GIT_RETENTION_TTL = RetainedSourceRetentionSettings.DEFAULT_GIT_RETENTION_TTL;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .findAndRegisterModules();

    private RetainedSourceSupport() {
    }

    public static RetainedSourceHandleRecord retainLocalPath(Path retainedRoot, String repositoryId, String sourceRevision) {
        Instant createdAt = Instant.now();
        return new RetainedSourceHandleRecord(
            newSourceHandle(),
            normalize(retainedRoot),
            RETAINED_ROOT_KIND_LOCAL_PATH_REFERENCE,
            ACQUISITION_TYPE_LOCAL_PATH,
            repositoryId,
            null,
            null,
            sourceRevision,
            createdAt,
            null,
            createdAt,
            RETENTION_POLICY_LOCAL_PATH_REFERENCE
        );
    }

    public static RetainedSourceHandleRecord retainGitCheckout(
        Path temporaryWorkspace,
        Path retentionWorkspaceDirectory,
        String repositoryId,
        String gitUrl,
        String gitRef,
        String sourceRevision
    ) {
        return retainGitCheckout(
            temporaryWorkspace,
            retentionWorkspaceDirectory,
            repositoryId,
            gitUrl,
            gitRef,
            sourceRevision,
            GIT_RETENTION_TTL
        );
    }

    public static RetainedSourceHandleRecord retainGitCheckout(
        Path temporaryWorkspace,
        Path retentionWorkspaceDirectory,
        String repositoryId,
        String gitUrl,
        String gitRef,
        String sourceRevision,
        Duration retentionTtl
    ) {
        if (retentionTtl == null || retentionTtl.isNegative() || retentionTtl.isZero()) {
            throw new IllegalArgumentException("retentionTtl must be > 0");
        }
        String sourceHandle = newSourceHandle();
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(retentionTtl);
        Path targetWorkspace = retentionRootsDirectory(retentionWorkspaceDirectory).resolve(sourceHandle);
        try {
            Files.createDirectories(targetWorkspace.getParent());
            Files.move(normalize(temporaryWorkspace), targetWorkspace, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to retain temporary Git workspace for source viewing", exception);
        }

        Path retainedRoot = targetWorkspace.resolve("repo");
        return new RetainedSourceHandleRecord(
            sourceHandle,
            retainedRoot,
            RETAINED_ROOT_KIND_RETAINED_GIT_CHECKOUT,
            ACQUISITION_TYPE_GIT,
            repositoryId,
            gitUrl,
            gitRef,
            sourceRevision,
            createdAt,
            expiresAt,
            createdAt,
            RETENTION_POLICY_TTL_7D
        );
    }

    public static void writeHandleRecord(Path retentionWorkspaceDirectory, RetainedSourceHandleRecord record) {
        Path handlesDirectory = retentionHandlesDirectory(retentionWorkspaceDirectory);
        try {
            Files.createDirectories(handlesDirectory);
            Files.write(recordPath(handlesDirectory, record.sourceHandle()), OBJECT_MAPPER.writeValueAsBytes(record.toRegistryMap()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write retained source handle record", exception);
        }
    }

    public static Path retentionHandlesDirectory(Path retentionWorkspaceDirectory) {
        return normalize(retentionWorkspaceDirectory).resolve("source-retention").resolve("handles");
    }

    public static Path retentionRootsDirectory(Path retentionWorkspaceDirectory) {
        return normalize(retentionWorkspaceDirectory).resolve("source-retention").resolve("roots");
    }

    private static Path recordPath(Path handlesDirectory, String sourceHandle) {
        return handlesDirectory.resolve(sourceHandle + ".json");
    }

    private static String newSourceHandle() {
        return "src_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }
}
