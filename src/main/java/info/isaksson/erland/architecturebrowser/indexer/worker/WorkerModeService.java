package info.isaksson.erland.architecturebrowser.indexer.worker;

import info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerModeService {
    private static final Logger LOG = Logger.getLogger(WorkerModeService.class.getName());


    public WorkerJobResult runJob(Path requestPath, Path resultPath) throws Exception {
        WorkerJobRequest request = WorkerJobJson.readRequest(requestPath);
        return runJob(request, resultPath);
    }

    public WorkerJobResult runJob(WorkerJobRequest request, Path resultPath) throws Exception {
        Instant startedAt = Instant.now();

        List<String> args = new ArrayList<>();
        if (request.repositoryId() != null && !request.repositoryId().isBlank()) {
            args.add("--repository-id");
            args.add(request.repositoryId());
        }
        if (request.sourcePath() != null && !request.sourcePath().isBlank()) {
            args.add("--source");
            args.add(request.sourcePath());
        } else if (request.gitUrl() != null && !request.gitUrl().isBlank()) {
            args.add("--git-url");
            args.add(request.gitUrl());
        } else {
            throw new IllegalArgumentException("Worker job must provide either sourcePath or gitUrl");
        }
        if (request.gitRef() != null && !request.gitRef().isBlank()) {
            args.add("--git-ref");
            args.add(request.gitRef());
        }
        if (request.outputPath() == null || request.outputPath().isBlank()) {
            throw new IllegalArgumentException("Worker job must provide outputPath");
        }
        args.add("--output");
        args.add(request.outputPath());

        if (request.snapshotIn() != null && !request.snapshotIn().isBlank()) {
            args.add("--snapshot-in");
            args.add(request.snapshotIn());
        }
        if (request.snapshotOut() != null && !request.snapshotOut().isBlank()) {
            args.add("--snapshot-out");
            args.add(request.snapshotOut());
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("arguments", List.copyOf(args));
        summary.put("repositoryId", request.repositoryId());
        summary.put("sourcePath", request.sourcePath());
        summary.put("gitUrl", request.gitUrl());
        summary.put("gitRef", request.gitRef());
        summary.put("outputPath", request.outputPath());
        summary.put("snapshotIn", request.snapshotIn());
        summary.put("snapshotOut", request.snapshotOut());
        String status = "SUCCESS";
        LOG.info(() -> "Starting worker job: jobId=" + safe(request.jobId())
            + ", repositoryId=" + safe(request.repositoryId())
            + ", gitUrl=" + safe(request.gitUrl())
            + ", gitRef=" + safe(request.gitRef())
            + ", sourcePath=" + safe(request.sourcePath())
            + ", outputPath=" + safe(request.outputPath()));
        try {
            IndexerCli.main(args.toArray(String[]::new));
            summary.put("message", "Worker job completed");
            summary.put("elapsedMs", Duration.between(startedAt, Instant.now()).toMillis());
            LOG.info(() -> "Completed worker job: jobId=" + safe(request.jobId())
                + ", outputPath=" + safe(request.outputPath())
                + ", elapsedMs=" + summary.get("elapsedMs"));
        } catch (Throwable ex) {
            status = "FAILED";
            summary.put("message", ex.getMessage());
            summary.put("exceptionType", ex.getClass().getName());
            Throwable rootCause = rootCauseOf(ex);
            if (rootCause != ex) {
                summary.put("rootCauseType", rootCause.getClass().getName());
                summary.put("rootCauseMessage", rootCause.getMessage());
            }
            summary.put("elapsedMs", Duration.between(startedAt, Instant.now()).toMillis());
            LOG.log(Level.SEVERE, "Worker job failed: jobId=" + safe(request.jobId())
                + ", repositoryId=" + safe(request.repositoryId())
                + ", gitUrl=" + safe(request.gitUrl())
                + ", gitRef=" + safe(request.gitRef())
                + ", sourcePath=" + safe(request.sourcePath()), ex);
            WorkerJobResult result = new WorkerJobResult(
                request.jobId(),
                status,
                startedAt,
                Instant.now(),
                request.outputPath(),
                summary
            );
            if (resultPath != null) {
                WorkerJobJson.writeResult(resultPath, result);
            }
            if (ex instanceof Exception exception) {
                throw exception;
            }
            if (ex instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(ex);
        }

        WorkerJobResult result = new WorkerJobResult(
            request.jobId(),
            status,
            startedAt,
            Instant.now(),
            request.outputPath(),
            summary
        );
        if (resultPath != null) {
            WorkerJobJson.writeResult(resultPath, result);
        }
        return result;
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }
}
