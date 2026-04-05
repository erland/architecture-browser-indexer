package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.nio.file.Path;

public record RetainedSourceResolvedFile(
    RetainedSourceHandleRecord sourceRecord,
    String relativePath,
    Path resolvedFile,
    long fileSizeBytes
) {
    public RetainedSourceResolvedFile {
        if (sourceRecord == null) {
            throw new IllegalArgumentException("sourceRecord is required");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
        if (resolvedFile == null) {
            throw new IllegalArgumentException("resolvedFile is required");
        }
        resolvedFile = resolvedFile.toAbsolutePath().normalize();
    }
}
