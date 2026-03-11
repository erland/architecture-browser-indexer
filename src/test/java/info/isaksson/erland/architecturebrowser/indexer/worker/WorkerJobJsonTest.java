package info.isaksson.erland.architecturebrowser.indexer.worker;

import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerJobJsonTest {

    @Test
    void readsRequestAndWritesResultJson() throws Exception {
        Path requestFile = Files.createTempFile("ab-worker-request", ".json");
        Files.writeString(requestFile, """
            {
              "jobId": "job-001",
              "repositoryId": "architecture-browser-indexer",
              "sourcePath": "/workspace",
              "outputPath": "/workspace/output/index.json",
              "snapshotOut": "/workspace/output/snapshot.json"
            }
            """);

        WorkerJobRequest request = WorkerJobJson.readRequest(requestFile);
        assertEquals("job-001", request.jobId());
        assertEquals("/workspace", request.sourcePath());

        Path resultFile = Files.createTempFile("ab-worker-result", ".json");
        WorkerJobJson.writeResult(resultFile, new WorkerJobResult(
            "job-001",
            "SUCCESS",
            Instant.parse("2026-03-11T13:30:00Z"),
            Instant.parse("2026-03-11T13:31:00Z"),
            "/workspace/output/index.json",
            Map.of("message", "ok")
        ));

        String resultJson = Files.readString(resultFile);
        assertEquals(true, resultJson.contains("\"jobId\""));
        assertEquals(true, resultJson.contains("\"SUCCESS\""));
    }
}
