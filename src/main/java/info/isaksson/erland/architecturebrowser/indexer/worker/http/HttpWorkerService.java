package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.worker.WorkerModeService;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceCleanupReport;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceCleanupService;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceFileAccessService;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceResolvedFile;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.SourceLanguageDetectionService;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportSnapshotSourceFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class HttpWorkerService {
    private final WorkerModeService workerModeService;
    private final Path workspaceDirectory;
    private final RetainedSourceFileAccessService retainedSourceFileAccessService;
    private final SourceLanguageDetectionService sourceLanguageDetectionService;
    private final RetainedSourceCleanupService retainedSourceCleanupService;

    public HttpWorkerService(Path workspaceDirectory) {
        this(new WorkerModeService(workspaceDirectory), workspaceDirectory);
    }

    HttpWorkerService(WorkerModeService workerModeService, Path workspaceDirectory) {
        this(workerModeService, workspaceDirectory, new RetainedSourceFileAccessService(workspaceDirectory), new SourceLanguageDetectionService());
    }

    HttpWorkerService(WorkerModeService workerModeService, Path workspaceDirectory, RetainedSourceFileAccessService retainedSourceFileAccessService) {
        this(workerModeService, workspaceDirectory, retainedSourceFileAccessService, new SourceLanguageDetectionService());
    }

    HttpWorkerService(WorkerModeService workerModeService, Path workspaceDirectory, RetainedSourceFileAccessService retainedSourceFileAccessService, SourceLanguageDetectionService sourceLanguageDetectionService) {
        this(workerModeService, workspaceDirectory, retainedSourceFileAccessService, sourceLanguageDetectionService,
            new RetainedSourceCleanupService(new info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceHandleRegistryService(workspaceDirectory)));
    }

    HttpWorkerService(
        WorkerModeService workerModeService,
        Path workspaceDirectory,
        RetainedSourceFileAccessService retainedSourceFileAccessService,
        SourceLanguageDetectionService sourceLanguageDetectionService,
        RetainedSourceCleanupService retainedSourceCleanupService
    ) {
        this.workerModeService = workerModeService;
        this.workspaceDirectory = workspaceDirectory.toAbsolutePath().normalize();
        this.retainedSourceFileAccessService = retainedSourceFileAccessService;
        this.sourceLanguageDetectionService = sourceLanguageDetectionService;
        this.retainedSourceCleanupService = retainedSourceCleanupService;
    }

    public HttpWorkerRunResponse runJob(WorkerJobRequest request) throws Exception {
        Files.createDirectories(workspaceDirectory);
        pruneRetainedSourcesBestEffort();
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
            result.summary(),
            HttpWorkerSourceAccessMapper.fromSummary(result.summary()),
            readSnapshotSourceFiles(outputPath)
        );
    }


    public HttpWorkerSourceFileReadResponse readSourceFile(HttpWorkerSourceFileReadRequest request) {
        pruneRetainedSourcesBestEffort();
        if (request == null) {
            throw new IllegalArgumentException("Source-file read request is required");
        }
        RetainedSourceResolvedFile resolvedFile = retainedSourceFileAccessService.resolveActiveTextFile(request.sourceHandle(), request.path());
        String sourceText = retainedSourceFileAccessService.readUtf8Text(resolvedFile);
        int totalLineCount = countLines(sourceText);
        return new HttpWorkerSourceFileReadResponse(
            resolvedFile.sourceRecord().sourceHandle(),
            resolvedFile.relativePath(),
            sourceLanguageDetectionService.detectLanguage(resolvedFile.relativePath()),
            totalLineCount,
            resolvedFile.fileSizeBytes(),
            request.startLine(),
            request.endLine(),
            sourceText
        );
    }


    RetainedSourceCleanupReport pruneRetainedSourcesBestEffort() {
        try {
            return retainedSourceCleanupService.pruneExpiredAndInvalid();
        } catch (RuntimeException exception) {
            return RetainedSourceCleanupReport.empty(Instant.now());
        }
    }


    private static ExportSnapshotSourceFiles readSnapshotSourceFiles(Path outputPath) {
        String fileName = outputPath.getFileName().toString();
        String artifactName = fileName.endsWith(".json")
            ? fileName.substring(0, fileName.length() - ".json".length()) + ".source-files.json"
            : fileName + ".source-files.json";
        Path artifactPath = outputPath.resolveSibling(artifactName);
        if (!Files.exists(artifactPath)) {
            return null;
        }
        try (var inputStream = Files.newInputStream(artifactPath)) {
            return HttpWorkerJson.readValue(inputStream, ExportSnapshotSourceFiles.class);
        } catch (IOException ex) {
            return null;
        }
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

    private static int countLines(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return 0;
        }
        int lineCount = 1;
        for (int index = 0; index < sourceText.length(); index++) {
            if (sourceText.charAt(index) == '\n') {
                lineCount++;
            }
        }
        return lineCount;
    }
}

