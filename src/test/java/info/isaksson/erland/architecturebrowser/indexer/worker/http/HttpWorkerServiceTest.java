package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.worker.WorkerModeService;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpWorkerServiceTest {

    @Test
    void assignsJobIdAndOutputPathWhenMissing() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-test");
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeService(), workspace);

        HttpWorkerRunResponse response = service.runJob(new WorkerJobRequest(
            null,
            "repo-1",
            "/workspace/repo",
            null,
            null,
            null,
            null,
            null
        ));

        assertNotNull(response.jobId());
        assertEquals("SUCCESS", response.status());
        assertTrue(response.outputPath().endsWith("architecture-index.json"));
        assertEquals("repo-1", response.document().source().repositoryId());
        assertEquals("SUCCESS", response.manifest().get("status"));
    }

    private static final class StubWorkerModeService extends WorkerModeService {
        @Override
        public WorkerJobResult runJob(WorkerJobRequest request, Path resultPath) throws Exception {
            Files.createDirectories(Path.of(request.outputPath()).getParent());
            Files.writeString(Path.of(request.outputPath()), minimalDocumentJson(request.repositoryId()));
            Files.writeString(Path.of(request.outputPath()).resolveSibling("architecture-index.manifest.json"), "{\n  \"status\" : \"SUCCESS\"\n}");
            return new WorkerJobResult(
                request.jobId(),
                "SUCCESS",
                Instant.parse("2026-03-11T19:00:00Z"),
                Instant.parse("2026-03-11T19:00:05Z"),
                request.outputPath(),
                Map.of("message", "ok")
            );
        }

        private static String minimalDocumentJson(String repositoryId) {
            return String.format("""
                {
                  "schemaVersion" : "1.0.0",
                  "indexerVersion" : "0.1.0-SNAPSHOT",
                  "source" : {
                    "repositoryId" : "%s",
                    "acquisitionType" : "LOCAL_PATH",
                    "path" : "/workspace/repo",
                    "branch" : null,
                    "revision" : null,
                    "remoteUrl" : null,
                    "acquiredAt" : "2026-03-11T19:00:00Z"
                  },
                  "runMetadata" : {
                    "startedAt" : "2026-03-11T19:00:00Z",
                    "completedAt" : "2026-03-11T19:00:05Z",
                    "outcome" : "SUCCESS",
                    "repository" : {
                      "repositoryId" : "%s",
                      "acquisitionType" : "LOCAL_PATH",
                      "path" : "/workspace/repo",
                      "branch" : null,
                      "revision" : null,
                      "remoteUrl" : null,
                      "acquiredAt" : "2026-03-11T19:00:00Z"
                    },
                    "detectedTechnologies" : [ ],
                    "fileStats" : {
                      "indexedFileCount" : 0,
                      "totalFileCount" : 0,
                      "degradedFileCount" : 0,
                      "omittedPaths" : [ ]
                    },
                    "warnings" : [ ],
                    "metadata" : { }
                  },
                  "completeness" : {
                    "status" : "COMPLETE",
                    "reasons" : [ ],
                    "degradedPaths" : [ ],
                    "missingCapabilities" : [ ]
                  },
                  "scopes" : [ ],
                  "entities" : [ ],
                  "relationships" : [ ],
                  "diagnostics" : [ ],
                  "metadata" : { }
                }
                """, repositoryId, repositoryId);
        }
    }
}
