package info.isaksson.erland.architecturebrowser.indexer.acquisition;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GitSourceAcquirer {
    AcquisitionResult acquire(AcquisitionRequest request) {
        if (request.gitUrl() == null || request.gitUrl().isBlank()) {
            throw new IllegalArgumentException("gitUrl must be provided");
        }

        Path workspace = createWorkspace(request.workingDirectory());
        Path cloneDirectory = workspace.resolve("repo");
        GitClient.cloneRepository(request.gitUrl(), request.gitRef(), cloneDirectory);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("gitCloneDirectory", cloneDirectory.toString());
        metadata.put("gitRemoteUrl", request.gitUrl());
        GitClient.appendGitMetadata(cloneDirectory, metadata);

        String repositoryId = RepositoryIdentity.resolve(request.repositoryId(), inferRepositoryName(request.gitUrl()));
        RepositorySource repositorySource = new RepositorySource(
            repositoryId,
            "git",
            cloneDirectory.toString(),
            request.gitUrl(),
            metadata.get("gitBranch") instanceof String branch ? branch : request.gitRef(),
            (String) metadata.get("gitRevision"),
            Instant.now(),
            Map.copyOf(metadata)
        );

        return new AcquisitionResult(repositorySource, cloneDirectory, List.of(), true);
    }

    private static Path createWorkspace(Path workingDirectory) {
        try {
            if (workingDirectory != null) {
                Files.createDirectories(workingDirectory);
                return Files.createTempDirectory(workingDirectory, "architecture-browser-indexer-");
            }
            return Files.createTempDirectory("architecture-browser-indexer-");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create Git acquisition workspace", exception);
        }
    }

    private static String inferRepositoryName(String gitUrl) {
        String trimmed = gitUrl == null ? "repository" : gitUrl.trim();
        int slashIndex = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf(':'));
        String candidate = slashIndex >= 0 && slashIndex + 1 < trimmed.length() ? trimmed.substring(slashIndex + 1) : trimmed;
        return candidate.replaceAll("\\.git$", "");
    }
}
