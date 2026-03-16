package info.isaksson.erland.architecturebrowser.indexer.application;

import java.nio.file.Path;

public record IndexRunRequest(
    String applicationVersion,
    String repositoryId,
    Path sourcePath,
    String gitUrl,
    String gitRef,
    Path workingDirectory,
    Path outputPath,
    String snapshotIn,
    String snapshotOut
) {
}
