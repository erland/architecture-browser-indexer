package info.isaksson.erland.architecturebrowser.indexer.worker.http;

/**
 * Contract placeholder for the planned source-file read endpoint.
 *
 * <p>The request is intentionally keyed by source handle and repository-relative
 * path. Absolute paths must never cross the platform/indexer boundary.</p>
 */
public record HttpWorkerSourceFileReadRequest(
    String sourceHandle,
    String path,
    Integer startLine,
    Integer endLine
) {
}
