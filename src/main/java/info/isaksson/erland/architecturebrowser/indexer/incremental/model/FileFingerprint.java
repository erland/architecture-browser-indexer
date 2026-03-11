package info.isaksson.erland.architecturebrowser.indexer.incremental.model;

public record FileFingerprint(
    String relativePath,
    long sizeBytes,
    String contentHash,
    String detectedLanguage,
    String fileType
) {
}
