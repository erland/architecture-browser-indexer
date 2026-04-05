package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RetainedSourceHandleRegistryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .findAndRegisterModules();

    private final Path retentionWorkspaceDirectory;
    private final RetainedSourceRetentionSettings retentionSettings;

    public RetainedSourceHandleRegistryService(Path retentionWorkspaceDirectory) {
        this(retentionWorkspaceDirectory, RetainedSourceRetentionSettings.defaults());
    }

    public RetainedSourceHandleRegistryService(Path retentionWorkspaceDirectory, RetainedSourceRetentionSettings retentionSettings) {
        this.retentionWorkspaceDirectory = normalize(retentionWorkspaceDirectory);
        this.retentionSettings = retentionSettings == null ? RetainedSourceRetentionSettings.defaults() : retentionSettings;
    }

    public RetainedSourceHandleRecord createLocalPathRecord(Path retainedRoot, String repositoryId, String sourceRevision) {
        return RetainedSourceSupport.retainLocalPath(retainedRoot, repositoryId, sourceRevision);
    }

    public RetainedSourceHandleRecord createRetainedGitCheckout(
        Path temporaryWorkspace,
        String repositoryId,
        String gitUrl,
        String gitRef,
        String sourceRevision
    ) {
        requireWorkspaceDirectory();
        return RetainedSourceSupport.retainGitCheckout(
            temporaryWorkspace,
            retentionWorkspaceDirectory,
            repositoryId,
            gitUrl,
            gitRef,
            sourceRevision,
            retentionSettings.gitRetentionTtl()
        );
    }

    public RetainedSourceHandleRecord save(RetainedSourceHandleRecord record) {
        requireWorkspaceDirectory();
        if (record == null) {
            throw new IllegalArgumentException("Retained source handle record is required");
        }
        validateRecordForPersistence(record);
        RetainedSourceSupport.writeHandleRecord(retentionWorkspaceDirectory, record);
        return record;
    }

    public Optional<RetainedSourceHandleRecord> find(String sourceHandle) {
        requireWorkspaceDirectory();
        String normalizedHandle = requireHandle(sourceHandle);
        Path recordPath = handleRecordPath(normalizedHandle);
        if (!Files.exists(recordPath)) {
            return Optional.empty();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(recordPath.toFile());
            return Optional.of(fromJson(root));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read retained source handle record for sourceHandle=" + normalizedHandle, exception);
        }
    }

    public RetainedSourceHandleRecord getRequired(String sourceHandle) {
        return find(sourceHandle).orElseThrow(() -> new IllegalArgumentException(
            "Unknown retained source handle: " + sourceHandle
        ));
    }

    public RetainedSourceHandleRecord getActive(String sourceHandle) {
        return getActive(sourceHandle, Instant.now());
    }

    public RetainedSourceHandleRecord getActive(String sourceHandle, Instant now) {
        RetainedSourceHandleRecord record = getRequired(sourceHandle);
        validateRecord(record, now == null ? Instant.now() : now);
        return record;
    }

    public RetainedSourceHandleRecord touch(String sourceHandle) {
        return touch(sourceHandle, Instant.now());
    }

    public RetainedSourceHandleRecord touch(String sourceHandle, Instant accessedAt) {
        RetainedSourceHandleRecord record = getRequired(sourceHandle);
        Instant touchedAt = accessedAt == null ? Instant.now() : accessedAt;
        RetainedSourceHandleRecord updated = new RetainedSourceHandleRecord(
            record.sourceHandle(),
            record.retainedRoot(),
            record.retainedRootKind(),
            record.acquisitionType(),
            record.repositoryId(),
            record.gitUrl(),
            record.gitRef(),
            record.sourceRevision(),
            record.createdAt(),
            record.expiresAt(),
            touchedAt,
            record.retentionPolicy()
        );
        return save(updated);
    }

    public List<RetainedSourceHandleRecord> list() {
        requireWorkspaceDirectory();
        Path handlesDirectory = RetainedSourceSupport.retentionHandlesDirectory(retentionWorkspaceDirectory);
        if (!Files.exists(handlesDirectory)) {
            return List.of();
        }
        try (var stream = Files.list(handlesDirectory)) {
            return stream
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .map(path -> find(stripJsonSuffix(path.getFileName().toString())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list retained source handle records", exception);
        }
    }

    public List<RetainedSourceHandleRecord> findExpired(Instant now) {
        Instant referenceTime = now == null ? Instant.now() : now;
        List<RetainedSourceHandleRecord> expired = new ArrayList<>();
        for (RetainedSourceHandleRecord record : list()) {
            if (isExpired(record, referenceTime)) {
                expired.add(record);
            }
        }
        return List.copyOf(expired);
    }


    public RetainedSourceRetentionSettings retentionSettings() {
        return retentionSettings;
    }

    public boolean delete(String sourceHandle) {
        requireWorkspaceDirectory();
        Optional<RetainedSourceHandleRecord> existing = find(sourceHandle);
        boolean deleted = false;
        if (existing.isPresent()) {
            RetainedSourceHandleRecord record = existing.get();
            if (RetainedSourceSupport.RETAINED_ROOT_KIND_RETAINED_GIT_CHECKOUT.equals(record.retainedRootKind())) {
                Path rootContainer = RetainedSourceSupport.retentionRootsDirectory(retentionWorkspaceDirectory).resolve(record.sourceHandle());
                deleted = deleteRecursively(rootContainer) || deleted;
            }
        }
        deleted = deleteIfExists(handleRecordPath(requireHandle(sourceHandle))) || deleted;
        return deleted;
    }

    public void validateRecord(RetainedSourceHandleRecord record, Instant now) {
        validateRecordForPersistence(record);
        if (isExpired(record, now == null ? Instant.now() : now)) {
            throw new IllegalArgumentException("Retained source handle has expired: " + record.sourceHandle());
        }
    }

    private void validateRecordForPersistence(RetainedSourceHandleRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Retained source handle record is required");
        }
        if (isBlank(record.sourceHandle())) {
            throw new IllegalArgumentException("Retained source handle record must contain sourceHandle");
        }
        if (record.retainedRoot() == null) {
            throw new IllegalArgumentException("Retained source handle record must contain retainedRoot");
        }
        if (!Files.exists(record.retainedRoot())) {
            throw new IllegalArgumentException("Retained source root does not exist for sourceHandle=" + record.sourceHandle());
        }
        if (!Files.isDirectory(record.retainedRoot())) {
            throw new IllegalArgumentException("Retained source root must be a directory for sourceHandle=" + record.sourceHandle());
        }
        if (isBlank(record.retainedRootKind())) {
            throw new IllegalArgumentException("Retained source handle record must contain retainedRootKind");
        }
        if (isBlank(record.acquisitionType())) {
            throw new IllegalArgumentException("Retained source handle record must contain acquisitionType");
        }
        if (isBlank(record.retentionPolicy())) {
            throw new IllegalArgumentException("Retained source handle record must contain retentionPolicy");
        }
    }

    public boolean isExpired(RetainedSourceHandleRecord record, Instant now) {
        if (record == null || record.expiresAt() == null) {
            return false;
        }
        Instant referenceTime = now == null ? Instant.now() : now;
        return !record.expiresAt().isAfter(referenceTime);
    }

    public Path handleRecordPath(String sourceHandle) {
        requireWorkspaceDirectory();
        return RetainedSourceSupport.retentionHandlesDirectory(retentionWorkspaceDirectory)
            .resolve(requireHandle(sourceHandle) + ".json");
    }

    private RetainedSourceHandleRecord fromJson(JsonNode root) {
        Path retainedRoot = root.hasNonNull("retainedRoot") ? Path.of(root.get("retainedRoot").asText()) : null;
        return new RetainedSourceHandleRecord(
            text(root, "sourceHandle"),
            retainedRoot,
            text(root, "retainedRootKind"),
            text(root, "acquisitionType"),
            text(root, "repositoryId"),
            text(root, "gitUrl"),
            text(root, "gitRef"),
            text(root, "sourceRevision"),
            instant(root, "createdAt"),
            instant(root, "expiresAt"),
            instant(root, "lastAccessedAt"),
            text(root, "retentionPolicy")
        );
    }

    private static String text(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.get(fieldName).asText() : null;
    }

    private static Instant instant(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? Instant.parse(node.get(fieldName).asText()) : null;
    }

    private static boolean deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return false;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(candidate -> deleteIfExists(candidate));
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete retained source path: " + path, exception);
        }
    }

    private static boolean deleteIfExists(Path path) {
        try {
            return path != null && Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete retained source path: " + path, exception);
        }
    }

    private void requireWorkspaceDirectory() {
        if (retentionWorkspaceDirectory == null) {
            throw new IllegalStateException("Retention workspace directory is required for retained source registry operations");
        }
    }

    private static String requireHandle(String sourceHandle) {
        if (isBlank(sourceHandle)) {
            throw new IllegalArgumentException("sourceHandle is required");
        }
        return sourceHandle.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private static String stripJsonSuffix(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }
}
