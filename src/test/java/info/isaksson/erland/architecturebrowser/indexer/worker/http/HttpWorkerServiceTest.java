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
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertNotNull(response.snapshotSourceFiles());
        assertEquals("snapshot-source-files/v1", response.snapshotSourceFiles().contractVersion());
    }

    @Test
    void runResponseDoesNotExposeLegacySourceAccess() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-test-no-source");
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeService(), workspace);

        HttpWorkerRunResponse response = service.runJob(new WorkerJobRequest(
            "job-2",
            "repo-1",
            "/workspace/repo",
            null,
            null,
            null,
            null,
            null
        ));

        Map<?, ?> serialized = HttpWorkerJson.readMap(HttpWorkerJson.writeBytes(response));
        assertNull(serialized.get("sourceAccess"));
        assertNotNull(response.snapshotSourceFiles());
    }

    private static final class StubWorkerModeService extends WorkerModeService {
        StubWorkerModeService() {
            super();
        }

        @Override
        public WorkerJobResult runJob(WorkerJobRequest request, Path resultPath) throws Exception {
            Files.createDirectories(Path.of(request.outputPath()).getParent());
            Files.writeString(Path.of(request.outputPath()), minimalDocumentJson(request.repositoryId()));
            Files.writeString(
                Path.of(request.outputPath()).resolveSibling("architecture-index.manifest.json"),
                """
                {
                  "status" : "SUCCESS"
                }
                """
            );
            Files.writeString(
                Path.of(request.outputPath()).resolveSibling("architecture-index.source-files.json"),
                """
                {
                  "contractVersion" : "snapshot-source-files/v1",
                  "files" : [ {
                    "relativePath" : "src/App.java",
                    "language" : "java",
                    "sizeBytes" : 12,
                    "totalLineCount" : 1,
                    "contentType" : "text/x-java-source",
                    "textContent" : "class App {}\\n"
                  } ],
                  "metadata" : { }
                }
                """
            );
            return new WorkerJobResult(
                request.jobId(),
                "SUCCESS",
                Instant.parse("2026-03-11T19:00:00Z"),
                Instant.parse("2026-03-11T19:00:05Z"),
                request.outputPath(),
                Map.of("message", "ok")
            );
        }
    }

    private static String minimalDocumentJson(String repositoryId) {
        return String.format("""
            {
              "schemaVersion" : "1.3.0",
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
                  "totalFiles" : 0,
                  "parsedFiles" : 0,
                  "skippedFiles" : 0,
                  "supportedFiles" : 0,
                  "unsupportedFiles" : 0
                },
                "errors" : [ ],
                "metadata" : { }
              },
              "scopes" : [ ],
              "entities" : [ ],
              "relationships" : [ ],
              "diagnostics" : [ ],
              "viewpoints" : [ ]
            }
            """, repositoryId, repositoryId);
    }
}
