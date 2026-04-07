package info.isaksson.erland.architecturebrowser.indexer.publish;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads referenced text files from the indexed source tree using repository-relative paths.
 */
public final class SnapshotSourceFileReader {
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 1024L * 1024L;
    public static final int DEFAULT_MAX_REFERENCED_FILES = 500;
    private static final int BINARY_SNIFF_BYTES = 8192;

    private final long maxFileSizeBytes;
    private final int maxReferencedFiles;
    private final SnapshotSourceFileMetadataService metadataService;

    public SnapshotSourceFileReader() {
        this(DEFAULT_MAX_FILE_SIZE_BYTES, DEFAULT_MAX_REFERENCED_FILES);
    }

    public SnapshotSourceFileReader(long maxFileSizeBytes) {
        this(maxFileSizeBytes, DEFAULT_MAX_REFERENCED_FILES);
    }

    public SnapshotSourceFileReader(long maxFileSizeBytes, int maxReferencedFiles) {
        this(maxFileSizeBytes, maxReferencedFiles, new SnapshotSourceFileMetadataService());
    }

    SnapshotSourceFileReader(long maxFileSizeBytes, int maxReferencedFiles, SnapshotSourceFileMetadataService metadataService) {
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("maxFileSizeBytes must be > 0");
        }
        if (maxReferencedFiles <= 0) {
            throw new IllegalArgumentException("maxReferencedFiles must be > 0");
        }
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxReferencedFiles = maxReferencedFiles;
        this.metadataService = metadataService;
    }

    public SnapshotSourceFileReadResult readReferencedTextFiles(Path indexedSourceRoot, List<String> referencedRelativePaths) {
        if (referencedRelativePaths == null || referencedRelativePaths.isEmpty()) {
            return new SnapshotSourceFileReadResult(List.of(), List.of(), List.of());
        }
        if (indexedSourceRoot == null) {
            List<SnapshotSourceFileSkip> skipped = referencedRelativePaths.stream()
                .map(path -> new SnapshotSourceFileSkip(path, "missing_source_root"))
                .toList();
            return new SnapshotSourceFileReadResult(List.of(), skipped.stream().map(SnapshotSourceFileSkip::relativePath).toList(), skipped);
        }
        Path sourceRoot = indexedSourceRoot.toAbsolutePath().normalize();
        if (!Files.exists(sourceRoot) || !Files.isDirectory(sourceRoot)) {
            List<SnapshotSourceFileSkip> skipped = referencedRelativePaths.stream()
                .map(path -> new SnapshotSourceFileSkip(path, "missing_source_root"))
                .toList();
            return new SnapshotSourceFileReadResult(List.of(), skipped.stream().map(SnapshotSourceFileSkip::relativePath).toList(), skipped);
        }

        List<SnapshotSourceFileText> files = new ArrayList<>();
        List<SnapshotSourceFileSkip> skipped = new ArrayList<>();
        int processed = 0;
        for (String relativePath : referencedRelativePaths) {
            if (processed >= maxReferencedFiles) {
                skipped.add(new SnapshotSourceFileSkip(relativePath, "referenced_file_limit_exceeded"));
                continue;
            }
            String unwantedReason = unwantedFileReason(relativePath);
            if (unwantedReason != null) {
                skipped.add(new SnapshotSourceFileSkip(relativePath, unwantedReason));
                continue;
            }
            try {
                files.add(readTextFile(sourceRoot, relativePath));
                processed += 1;
            } catch (IllegalArgumentException ex) {
                skipped.add(new SnapshotSourceFileSkip(relativePath, classifyReason(ex.getMessage())));
            } catch (RuntimeException ex) {
                skipped.add(new SnapshotSourceFileSkip(relativePath, "read_error"));
            }
        }
        return new SnapshotSourceFileReadResult(files, skipped.stream().map(SnapshotSourceFileSkip::relativePath).toList(), skipped);
    }

    SnapshotSourceFileText readTextFile(Path indexedSourceRoot, String relativePath) {
        String normalizedRelativePath = SnapshotSourceFileReferenceCollector.normalizeRelativePath(relativePath);
        if (normalizedRelativePath == null) {
            throw new IllegalArgumentException("Referenced source path is invalid: " + relativePath);
        }
        Path sourceRoot = indexedSourceRoot.toAbsolutePath().normalize();
        Path candidate = sourceRoot.resolve(normalizedRelativePath).normalize();
        if (!candidate.startsWith(sourceRoot)) {
            throw new IllegalArgumentException("Referenced source path escapes the indexed source root: " + normalizedRelativePath);
        }
        if (!Files.exists(candidate)) {
            throw new IllegalArgumentException("Referenced source file does not exist: " + normalizedRelativePath);
        }
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Referenced source path must be a regular file: " + normalizedRelativePath);
        }
        Path realRoot = realPath(sourceRoot, "indexed source root");
        Path realCandidate = realPath(candidate, "referenced source file");
        if (!realCandidate.startsWith(realRoot)) {
            throw new IllegalArgumentException("Referenced source path resolves outside the indexed source root: " + normalizedRelativePath);
        }
        long fileSizeBytes = fileSize(realCandidate, normalizedRelativePath);
        if (fileSizeBytes > maxFileSizeBytes) {
            throw new IllegalArgumentException("Referenced source file exceeds max allowed size of " + maxFileSizeBytes + " bytes: " + normalizedRelativePath);
        }
        ensureLikelyTextFile(realCandidate, normalizedRelativePath);
        try {
            String textContent = Files.readString(realCandidate, StandardCharsets.UTF_8);
            String language = metadataService.detectLanguage(normalizedRelativePath);
            int totalLineCount = metadataService.countLines(textContent);
            String contentType = metadataService.detectContentType(normalizedRelativePath, language);
            return new SnapshotSourceFileText(normalizedRelativePath, textContent, fileSizeBytes, totalLineCount, language, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read referenced source file text: " + normalizedRelativePath, exception);
        }
    }

    private static String unwantedFileReason(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "invalid_path";
        }
        String normalized = relativePath.replace('\\', '/').toLowerCase();
        if (normalized.endsWith(".min.js") || normalized.endsWith(".min.css")) {
            return "minified_asset";
        }
        if (normalized.endsWith(".map")) {
            return "source_map";
        }
        return null;
    }

    private static String classifyReason(String message) {
        if (message == null || message.isBlank()) {
            return "invalid_source_file";
        }
        String value = message.toLowerCase();
        if (value.contains("invalid")) {
            return "invalid_path";
        }
        if (value.contains("escapes") || value.contains("outside the indexed source root")) {
            return "path_outside_source_root";
        }
        if (value.contains("does not exist")) {
            return "missing_file";
        }
        if (value.contains("regular file")) {
            return "not_regular_file";
        }
        if (value.contains("max allowed size") || value.contains("exceeds max allowed size")) {
            return "file_too_large";
        }
        if (value.contains("does not appear to be a text file")) {
            return "binary_or_non_text";
        }
        return "invalid_source_file";
    }

    private static void ensureLikelyTextFile(Path file, String relativePath) {
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] buffer = stream.readNBytes(BINARY_SNIFF_BYTES);
            for (byte value : buffer) {
                if (value == 0) {
                    throw new IllegalArgumentException("Referenced source file does not appear to be a text file: " + relativePath);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect referenced source file: " + relativePath, exception);
        }
    }

    private static long fileSize(Path file, String relativePath) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to determine referenced source file size: " + relativePath, exception);
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
