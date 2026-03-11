package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.worker.WorkerModeService;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public final class HttpWorkerService {
    private final WorkerModeService workerModeService;
    private final Path workspaceDirectory;

    public HttpWorkerService(Path workspaceDirectory) {
        this(new WorkerModeService(), workspaceDirectory);
    }

    HttpWorkerService(WorkerModeService workerModeService, Path workspaceDirectory) {
        this.workerModeService = workerModeService;
        this.workspaceDirectory = workspaceDirectory.toAbsolutePath().normalize();
    }

    public HttpWorkerRunResponse runJob(WorkerJobRequest request) throws Exception {
        Files.createDirectories(workspaceDirectory);
        String jobId = blankToNull(request.jobId()) == null ? UUID.randomUUID().toString() : request.jobId();
        Path jobDirectory = Files.createTempDirectory(workspaceDirectory, sanitizeJobId(jobId) + "-");
        Path outputPath = blankToNull(request.outputPath()) == null
            ? jobDirectory.resolve("architecture-index.json")
            : Path.of(request.outputPath()).toAbsolutePath().normalize();
        Path resultPath = jobDirectory.resolve("result.json");
        String snapshotOut = blankToNull(request.snapshotOut()) == null ? null : Path.of(request.snapshotOut()).toAbsolutePath().normalize().toString();

        WorkerJobRequest normalizedRequest = new WorkerJobRequest(
            jobId,
            request.repositoryId(),
            request.sourcePath(),
            request.gitUrl(),
            request.gitRef(),
            outputPath.toString(),
            request.snapshotIn(),
            snapshotOut
        );

        WorkerJobResult result = workerModeService.runJob(normalizedRequest, resultPath);
        ArchitectureIndexDocument document = ArchitectureIrJson.read(outputPath);

        return new HttpWorkerRunResponse(
            result.jobId(),
            result.status(),
            result.startedAt(),
            result.finishedAt(),
            result.outputPath(),
            normalizedRequest.snapshotOut(),
            document,
            readManifest(outputPath),
            result.summary()
        );
    }

    private static Map<String, Object> readManifest(Path outputPath) {
        String fileName = outputPath.getFileName().toString();
        String manifestName = fileName.endsWith(".json")
            ? fileName.substring(0, fileName.length() - ".json".length()) + ".manifest.json"
            : fileName + ".manifest.json";
        Path manifestPath = outputPath.resolveSibling(manifestName);
        if (!Files.exists(manifestPath)) {
            return Map.of();
        }
        try {
            return HttpWorkerJson.readMap(Files.readAllBytes(manifestPath));
        } catch (IOException ex) {
            return Map.of("manifestReadError", ex.getMessage(), "manifestPath", manifestPath.toString());
        }
    }

    private static String sanitizeJobId(String jobId) {
        return jobId.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
