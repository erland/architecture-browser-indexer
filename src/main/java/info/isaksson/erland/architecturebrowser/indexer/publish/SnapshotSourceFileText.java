package info.isaksson.erland.architecturebrowser.indexer.publish;

public record SnapshotSourceFileText(
    String relativePath,
    String textContent,
    long sizeBytes,
    int totalLineCount,
    String language,
    String contentType
) {
    public SnapshotSourceFileText {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
        if (textContent == null) {
            textContent = "";
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be non-negative");
        }
        if (totalLineCount < 0) {
            throw new IllegalArgumentException("totalLineCount must be non-negative");
        }
        if (language != null && language.isBlank()) {
            language = null;
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "text/plain";
        }
    }
}
