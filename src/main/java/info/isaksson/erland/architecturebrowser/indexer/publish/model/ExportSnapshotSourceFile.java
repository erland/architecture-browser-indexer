package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import java.util.Objects;

/**
 * Platform-facing export artifact for a single referenced source file captured for one snapshot.
 *
 * <p>This contract is intentionally snapshot-scoped and does not imply any cross-snapshot
 * deduplication. Each exported file should appear at most once within one snapshot export.</p>
 */
public record ExportSnapshotSourceFile(
    String relativePath,
    String language,
    long sizeBytes,
    int totalLineCount,
    String contentType,
    String textContent
) {
    public ExportSnapshotSourceFile {
        relativePath = normalizeRelativePath(relativePath);
        language = normalizeOptional(language);
        contentType = normalizeContentType(contentType);
        textContent = Objects.requireNonNullElse(textContent, "");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be non-negative");
        }
        if (totalLineCount < 0) {
            throw new IllegalArgumentException("totalLineCount must be non-negative");
        }
    }

    private static String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        String normalized = value.replace('\\', '/').trim();
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new IllegalArgumentException("relativePath must be repository-relative and must not escape the source root");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeContentType(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? "text/plain" : normalized;
    }
}
