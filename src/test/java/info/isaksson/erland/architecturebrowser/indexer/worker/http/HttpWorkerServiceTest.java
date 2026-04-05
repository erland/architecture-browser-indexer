package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.worker.WorkerModeService;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceFileAccessService;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceHandleRegistryService;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceHandleRecord;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertNotNull(response.sourceAccess());
        assertEquals("SOURCE_HANDLE", response.sourceAccess().lookupKeyKind());
        assertEquals("src_HANDLE_1", response.sourceAccess().sourceHandle());
        assertEquals("LOCAL_PATH_REFERENCE", response.sourceAccess().retainedRootKind());
    }

    @Test
    void leavesSourceAccessNullWhenSummaryDoesNotContainIt() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-test-no-source");
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeServiceWithoutSourceAccess(), workspace);

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

        assertNull(response.sourceAccess());
    }



    @Test
    void prunesExpiredHandlesBeforeSourceRead() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-prune-before-read");
        Path localRoot = Files.createTempDirectory("ab-http-worker-prune-before-read-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord expired = registry.save(new RetainedSourceHandleRecord(
            "src_EXPIRED_READ",
            localRoot,
            "LOCAL_PATH_REFERENCE",
            "LOCAL_PATH",
            "repo-expired",
            null,
            null,
            "rev-expired",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-02T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"),
            "ttl-7d"
        ));
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeServiceWithoutSourceAccess(), workspace, new RetainedSourceFileAccessService(registry));

        assertThrows(IllegalArgumentException.class, () -> service.readSourceFile(new HttpWorkerSourceFileReadRequest(
            expired.sourceHandle(),
            "App.java",
            1,
            1
        )));
        assertTrue(registry.find(expired.sourceHandle()).isEmpty());
    }

    @Test
    void prunesExpiredHandlesBeforeRunJob() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-prune-before-run");
        Path localRoot = Files.createTempDirectory("ab-http-worker-prune-before-run-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord expired = registry.save(new RetainedSourceHandleRecord(
            "src_EXPIRED_RUN",
            localRoot,
            "LOCAL_PATH_REFERENCE",
            "LOCAL_PATH",
            "repo-expired-run",
            null,
            null,
            "rev-expired-run",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-02T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"),
            "ttl-7d"
        ));
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeServiceWithoutSourceAccess(), workspace, new RetainedSourceFileAccessService(registry));

        HttpWorkerRunResponse response = service.runJob(new WorkerJobRequest(
            "job-prune",
            "repo-1",
            "/workspace/repo",
            null,
            null,
            null,
            null,
            null
        ));

        assertEquals("SUCCESS", response.status());
        assertTrue(registry.find(expired.sourceHandle()).isEmpty());
    }

    @Test
    void readsSourceFileFromRetainedSourceHandle() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-test-source-read");
        Path sourceRoot = Files.createTempDirectory("ab-http-worker-source-root");
        Path sourceFile = sourceRoot.resolve("src/App.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "class App {}\nvoid run() {}\n");

        RetainedSourceHandleRegistryService registryService = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord record = registryService.save(registryService.createLocalPathRecord(sourceRoot, "repo-1", "rev-1"));
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeServiceWithoutSourceAccess(), workspace, new RetainedSourceFileAccessService(registryService));

        HttpWorkerSourceFileReadResponse response = service.readSourceFile(new HttpWorkerSourceFileReadRequest(
            record.sourceHandle(),
            "src/App.java",
            1,
            2
        ));

        assertEquals(record.sourceHandle(), response.sourceHandle());
        assertEquals("src/App.java", response.path());
        assertEquals("java", response.language());
        assertEquals(3, response.totalLineCount());
        assertEquals(Files.size(sourceFile), response.fileSizeBytes());
        assertEquals(1, response.requestedStartLine());
        assertEquals(2, response.requestedEndLine());
        assertTrue(response.sourceText().contains("class App"));
    }

    @Test
    void rejectsMissingSourceFileRequest() throws Exception {
        Path workspace = Files.createTempDirectory("ab-http-worker-test-source-read-missing");
        HttpWorkerService service = new HttpWorkerService(new StubWorkerModeServiceWithoutSourceAccess(), workspace);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.readSourceFile(null));

        assertTrue(exception.getMessage().contains("required"));
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
                Map.of(
                    "message", "ok",
                    "sourceAccess", Map.of(
                        "lookupKeyKind", "SOURCE_HANDLE",
                        "sourceHandle", "src_HANDLE_1",
                        "retainedRootKind", "LOCAL_PATH_REFERENCE",
                        "acquisitionType", "LOCAL_PATH",
                        "repositoryId", request.repositoryId(),
                        "sourceRevision", "rev-123",
                        "retentionPolicy", "local-path-reference",
                        "createdAt", "2026-04-05T18:10:00Z"
                    )
                )
            );
        }
    }

    private static final class StubWorkerModeServiceWithoutSourceAccess extends WorkerModeService {
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
    }

    private static String minimalDocumentJson(String repositoryId) {
        return String.format("""
            {
              \"schemaVersion\" : \"1.3.0\",
              \"indexerVersion\" : \"0.1.0-SNAPSHOT\",
              \"source\" : {
                \"repositoryId\" : \"%s\",
                \"acquisitionType\" : \"LOCAL_PATH\",
                \"path\" : \"/workspace/repo\",
                \"branch\" : null,
                \"revision\" : null,
                \"remoteUrl\" : null,
                \"acquiredAt\" : \"2026-03-11T19:00:00Z\"
              },
              \"runMetadata\" : {
                \"startedAt\" : \"2026-03-11T19:00:00Z\",
                \"completedAt\" : \"2026-03-11T19:00:05Z\",
                \"outcome\" : \"SUCCESS\",
                \"repository\" : {
                  \"repositoryId\" : \"%s\",
                  \"acquisitionType\" : \"LOCAL_PATH\",
                  \"path\" : \"/workspace/repo\",
                  \"branch\" : null,
                  \"revision\" : null,
                  \"remoteUrl\" : null,
                  \"acquiredAt\" : \"2026-03-11T19:00:00Z\"
                },
                \"detectedTechnologies\" : [ ],
                \"fileStats\" : {
                  \"indexedFileCount\" : 0,
                  \"totalFileCount\" : 0,
                  \"degradedFileCount\" : 0,
                  \"omittedPaths\" : [ ]
                },
                \"warnings\" : [ ],
                \"metadata\" : { }
              },
              \"completeness\" : {
                \"status\" : \"COMPLETE\",
                \"reasons\" : [ ],
                \"degradedPaths\" : [ ],
                \"missingCapabilities\" : [ ]
              },
              \"scopes\" : [ ],
              \"entities\" : [ ],
              \"relationships\" : [ ],
              \"diagnostics\" : [ ],
              \"metadata\" : { }
            }
            """, repositoryId, repositoryId);
    }
}
