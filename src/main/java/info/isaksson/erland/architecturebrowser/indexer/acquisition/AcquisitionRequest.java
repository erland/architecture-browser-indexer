package info.isaksson.erland.architecturebrowser.indexer.acquisition;

import java.nio.file.Path;

public record AcquisitionRequest(
    String repositoryId,
    Path localPath,
    String gitUrl,
    String gitRef,
    Path workingDirectory
) {
    public boolean isLocalPathRequest() {
        return localPath != null;
    }

    public boolean isGitRequest() {
        return gitUrl != null && !gitUrl.isBlank();
    }
}
