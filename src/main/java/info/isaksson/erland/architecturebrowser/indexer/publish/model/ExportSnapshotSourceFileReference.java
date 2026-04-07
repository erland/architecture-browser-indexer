package info.isaksson.erland.architecturebrowser.indexer.publish.model;

/**
 * Lightweight metadata reference intended for future manifest and diagnostic summaries.
 */
public record ExportSnapshotSourceFileReference(
    String relativePath,
    String language,
    long sizeBytes,
    int totalLineCount
) {
}
