package info.isaksson.erland.architecturebrowser.indexer.worker.http;

/**
 * Contract placeholder for the planned source-file read endpoint.
 */
public record HttpWorkerSourceFileReadResponse(
    String sourceHandle,
    String path,
    String language,
    Integer totalLineCount,
    Long fileSizeBytes,
    Integer requestedStartLine,
    Integer requestedEndLine,
    String sourceText
) {
}
