package info.isaksson.erland.architecturebrowser.indexer.worker.model;

public record WorkerJobRequest(
    String jobId,
    String repositoryId,
    String sourcePath,
    String gitUrl,
    String gitRef,
    String outputPath,
    String snapshotIn,
    String snapshotOut
) {
}
