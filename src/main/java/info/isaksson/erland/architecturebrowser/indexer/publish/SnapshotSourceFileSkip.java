package info.isaksson.erland.architecturebrowser.indexer.publish;

public record SnapshotSourceFileSkip(
    String relativePath,
    String reason
) {
    public SnapshotSourceFileSkip {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
    }
}
