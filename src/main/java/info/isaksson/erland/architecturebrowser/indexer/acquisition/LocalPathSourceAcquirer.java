package info.isaksson.erland.architecturebrowser.indexer.acquisition;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LocalPathSourceAcquirer {
    AcquisitionResult acquire(AcquisitionRequest request) {
        Path normalizedPath = requireExistingDirectory(request.localPath()).toAbsolutePath().normalize();
        Instant acquiredAt = Instant.now();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("exists", Files.exists(normalizedPath));
        metadata.put("directory", Files.isDirectory(normalizedPath));

        boolean gitWorkingTree = GitClient.isGitWorkingTree(normalizedPath);
        metadata.put("gitWorkingTree", gitWorkingTree);
        if (gitWorkingTree) {
            GitClient.appendGitMetadata(normalizedPath, metadata);
        }

        String repositoryId = RepositoryIdentity.resolve(request.repositoryId(), normalizedPath.getFileName() != null
            ? normalizedPath.getFileName().toString()
            : normalizedPath.toString());

        RepositorySource repositorySource = new RepositorySource(
            repositoryId,
            "local-path",
            normalizedPath.toString(),
            null,
            (String) metadata.get("gitBranch"),
            (String) metadata.get("gitRevision"),
            acquiredAt,
            Map.copyOf(metadata)
        );

        return new AcquisitionResult(repositorySource, normalizedPath, List.of(), false);
    }

    private static Path requireExistingDirectory(Path sourcePath) {
        if (sourcePath == null) {
            throw new IllegalArgumentException("localPath must be provided");
        }
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Local path does not exist: " + sourcePath);
        }
        if (!Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException("Local path is not a directory: " + sourcePath);
        }
        return sourcePath;
    }
}
