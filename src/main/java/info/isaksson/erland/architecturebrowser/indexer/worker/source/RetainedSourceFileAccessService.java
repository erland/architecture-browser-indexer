package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;

public class RetainedSourceFileAccessService {
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 1024L * 1024L;
    private static final int BINARY_SNIFF_BYTES = 8192;

    private final RetainedSourceHandleRegistryService registryService;
    private final long maxFileSizeBytes;

    public RetainedSourceFileAccessService(Path retentionWorkspaceDirectory) {
        this(new RetainedSourceHandleRegistryService(retentionWorkspaceDirectory), DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    public RetainedSourceFileAccessService(RetainedSourceHandleRegistryService registryService) {
        this(registryService, DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    public RetainedSourceFileAccessService(RetainedSourceHandleRegistryService registryService, long maxFileSizeBytes) {
        if (registryService == null) {
            throw new IllegalArgumentException("registryService is required");
        }
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("maxFileSizeBytes must be > 0");
        }
        this.registryService = registryService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public RetainedSourceResolvedFile resolveActiveTextFile(String sourceHandle, String relativePath) {
        return resolveActiveTextFile(sourceHandle, relativePath, Instant.now());
    }

    public RetainedSourceResolvedFile resolveActiveTextFile(String sourceHandle, String relativePath, Instant now) {
        RetainedSourceHandleRecord record = registryService.getActive(sourceHandle, now);
        RetainedSourceResolvedFile resolvedFile = resolveTextFile(record, relativePath);
        registryService.touch(record.sourceHandle(), now == null ? Instant.now() : now);
        return resolvedFile;
    }

    public RetainedSourceResolvedFile resolveTextFile(RetainedSourceHandleRecord record, String relativePath) {
        if (record == null) {
            throw new IllegalArgumentException("Retained source handle record is required");
        }
        String normalizedRelativePath = normalizeRelativePath(relativePath);
        Path retainedRoot = record.retainedRoot().toAbsolutePath().normalize();
        Path candidate = retainedRoot.resolve(normalizedRelativePath).normalize();
        if (!candidate.startsWith(retainedRoot)) {
            throw new IllegalArgumentException("Requested path escapes the retained source root: " + normalizedRelativePath);
        }
        if (!Files.exists(candidate)) {
            throw new IllegalArgumentException("Requested source file does not exist: " + normalizedRelativePath);
        }
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Requested source path must be a regular file: " + normalizedRelativePath);
        }
        Path realRoot = realPath(retainedRoot, "retained source root");
        Path realCandidate = realPath(candidate, "requested source file");
        if (!realCandidate.startsWith(realRoot)) {
            throw new IllegalArgumentException("Requested path resolves outside the retained source root: " + normalizedRelativePath);
        }
        long fileSizeBytes = fileSize(realCandidate, normalizedRelativePath);
        if (fileSizeBytes > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                "Requested source file exceeds max allowed size of " + maxFileSizeBytes + " bytes: " + normalizedRelativePath
            );
        }
        ensureLikelyTextFile(realCandidate, normalizedRelativePath);
        return new RetainedSourceResolvedFile(record, normalizedRelativePath, realCandidate, fileSizeBytes);
    }

    public String readUtf8Text(RetainedSourceResolvedFile resolvedFile) {
        if (resolvedFile == null) {
            throw new IllegalArgumentException("resolvedFile is required");
        }
        try {
            return Files.readString(resolvedFile.resolvedFile(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to read retained source file text: " + resolvedFile.relativePath(),
                exception
            );
        }
    }

    public long maxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    static String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Requested source path is required");
        }
        String normalizedSeparators = relativePath.replace('\\', '/').trim();
        if (normalizedSeparators.isBlank()) {
            throw new IllegalArgumentException("Requested source path is required");
        }
        if (normalizedSeparators.startsWith("/") || normalizedSeparators.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Requested source path must be repository-relative: " + relativePath);
        }
        Path normalized = Path.of(normalizedSeparators).normalize();
        if (normalized.getNameCount() == 0) {
            throw new IllegalArgumentException("Requested source path is required");
        }
        if (normalized.isAbsolute()) {
            throw new IllegalArgumentException("Requested source path must be repository-relative: " + relativePath);
        }
        for (Path element : normalized) {
            if (element.toString().equals("..")) {
                throw new IllegalArgumentException("Requested source path must not contain parent traversal: " + relativePath);
            }
        }
        return normalized.toString().replace('\\', '/');
    }

    private static void ensureLikelyTextFile(Path file, String relativePath) {
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] buffer = stream.readNBytes(BINARY_SNIFF_BYTES);
            for (byte value : buffer) {
                if (value == 0) {
                    throw new IllegalArgumentException("Requested source file does not appear to be a text file: " + relativePath);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect retained source file: " + relativePath, exception);
        }
    }

    private static long fileSize(Path file, String relativePath) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to determine retained source file size: " + relativePath, exception);
        }
    }

    private static Path realPath(Path path, String description) {
        try {
            return path.toRealPath();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to resolve " + description + ": " + path, exception);
        }
    }
}
