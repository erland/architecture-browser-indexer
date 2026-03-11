package info.isaksson.erland.architecturebrowser.indexer.worker;

import info.isaksson.erland.architecturebrowser.indexer.cli.IndexerCli;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkerModeService {

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
        String status = "SUCCESS";
        try {
            IndexerCli.main(args.toArray(String[]::new));
            summary.put("message", "Worker job completed");
        } catch (Exception ex) {
            status = "FAILED";
            summary.put("message", ex.getMessage());
            summary.put("exceptionType", ex.getClass().getName());
            WorkerJobResult result = new WorkerJobResult(
                request.jobId(),
                status,
                startedAt,
                Instant.now(),
                request.outputPath(),
                Map.copyOf(summary)
            );
            if (resultPath != null) {
                WorkerJobJson.writeResult(resultPath, result);
            }
            throw ex;
        }

        WorkerJobResult result = new WorkerJobResult(
            request.jobId(),
            status,
            startedAt,
            Instant.now(),
            request.outputPath(),
            Map.copyOf(summary)
        );
        if (resultPath != null) {
            WorkerJobJson.writeResult(resultPath, result);
        }
        return result;
    }
}
