package info.isaksson.erland.architecturebrowser.indexer.worker.http;

import info.isaksson.erland.architecturebrowser.indexer.application.IndexRunResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.worker.WorkerModeService;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceHandleRecord;
import info.isaksson.erland.architecturebrowser.indexer.worker.source.RetainedSourceHandleRegistryService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceViewContractEndToEndTest {

    @Test
    void runEndpointAndSourceReadWorkTogetherForLocalPathRuns() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-view-e2e-local");
        Path localRoot = Files.createTempDirectory("ab-source-view-e2e-local-root");
        Path sourceFile = localRoot.resolve("src/main/java/com/example/App.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "package com.example;\nclass App {}\n");

        HttpWorkerService service = new HttpWorkerService(
            new EndToEndWorkerModeService(
                workspace,
                indexRunResult(localRoot, false, "LOCAL_PATH", "repo-local", "rev-local")
            ),
            workspace
        );

        HttpWorkerRunResponse runResponse = service.runJob(new WorkerJobRequest(
            "job-local",
            "repo-local",
            localRoot.toString(),
            null,
            null,
            workspace.resolve("local-out/architecture-index.json").toString(),
            null,
            null
        ));

        assertNotNull(runResponse.sourceAccess());
        assertEquals("SOURCE_HANDLE", runResponse.sourceAccess().lookupKeyKind());
        assertEquals("LOCAL_PATH_REFERENCE", runResponse.sourceAccess().retainedRootKind());
        assertEquals("repo-local", runResponse.sourceAccess().repositoryId());
        assertEquals("rev-local", runResponse.sourceAccess().sourceRevision());

        HttpWorkerSourceFileReadResponse sourceResponse = service.readSourceFile(new HttpWorkerSourceFileReadRequest(
            runResponse.sourceAccess().sourceHandle(),
            "src/main/java/com/example/App.java",
            1,
            2
        ));

        assertEquals(runResponse.sourceAccess().sourceHandle(), sourceResponse.sourceHandle());
        assertEquals("src/main/java/com/example/App.java", sourceResponse.path());
        assertEquals("java", sourceResponse.language());
        assertEquals(3, sourceResponse.totalLineCount());
        assertTrue(sourceResponse.sourceText().contains("class App {}"));

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord record = registry.getRequired(runResponse.sourceAccess().sourceHandle());
        assertEquals(localRoot.toAbsolutePath().normalize(), record.retainedRoot());
        assertNotNull(record.lastAccessedAt());
    }

    @Test
    void runEndpointAndSourceReadWorkTogetherForRetainedGitCheckouts() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-view-e2e-git");
        Path temporaryWorkspace = Files.createTempDirectory("ab-source-view-e2e-git-temp");
        Path acquiredRoot = temporaryWorkspace.resolve("repo");
        Path sourceFile = acquiredRoot.resolve("src/App.tsx");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "export const App = () => <div>Hello</div>;\n");

        HttpWorkerService service = new HttpWorkerService(
            new EndToEndWorkerModeService(
                workspace,
                indexRunResult(acquiredRoot, true, "GIT", "repo-git", "rev-git")
            ),
            workspace
        );

        HttpWorkerRunResponse runResponse = service.runJob(new WorkerJobRequest(
            "job-git",
            "repo-git",
            null,
            "https://github.com/example/repo.git",
            "main",
            workspace.resolve("git-out/architecture-index.json").toString(),
            null,
            null
        ));

        assertNotNull(runResponse.sourceAccess());
        assertEquals("RETAINED_GIT_CHECKOUT", runResponse.sourceAccess().retainedRootKind());
        assertEquals("GIT", runResponse.sourceAccess().acquisitionType());
        assertEquals("ttl-7d", runResponse.sourceAccess().retentionPolicy());
        assertFalse(Files.exists(temporaryWorkspace));

        HttpWorkerSourceFileReadResponse sourceResponse = service.readSourceFile(new HttpWorkerSourceFileReadRequest(
            runResponse.sourceAccess().sourceHandle(),
            "src/App.tsx",
            1,
            1
        ));

        assertEquals("tsx", sourceResponse.language());
        assertTrue(sourceResponse.sourceText().contains("export const App"));

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord record = registry.getRequired(runResponse.sourceAccess().sourceHandle());
        assertTrue(Files.exists(record.retainedRoot().resolve("src/App.tsx")));
        assertNotNull(record.expiresAt());
    }

    @Test
    void sourceReadRejectsTraversalWhenUsingRunEndpointHandle() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-view-e2e-traversal");
        Path localRoot = Files.createTempDirectory("ab-source-view-e2e-traversal-root");
        Path sourceFile = localRoot.resolve("src/main/java/com/example/App.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "class App {}\n");

        HttpWorkerService service = new HttpWorkerService(
            new EndToEndWorkerModeService(
                workspace,
                indexRunResult(localRoot, false, "LOCAL_PATH", "repo-traversal", "rev-traversal")
            ),
            workspace
        );

        HttpWorkerRunResponse runResponse = service.runJob(new WorkerJobRequest(
            "job-traversal",
            "repo-traversal",
            localRoot.toString(),
            null,
            null,
            workspace.resolve("traversal-out/architecture-index.json").toString(),
            null,
            null
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.readSourceFile(
            new HttpWorkerSourceFileReadRequest(runResponse.sourceAccess().sourceHandle(), "../secrets.txt", 1, 1)
        ));

        assertTrue(exception.getMessage().contains("repository-relative") || exception.getMessage().contains("parent traversal"));
    }

    private static IndexRunResult indexRunResult(Path acquiredRoot, boolean temporaryWorkspace, String acquisitionType, String repositoryId, String revision) {
        RepositorySource source = new RepositorySource(
            repositoryId,
            acquisitionType,
            acquiredRoot.toString(),
            "GIT".equals(acquisitionType) ? "https://github.com/example/repo.git" : null,
            "main",
            revision,
            Instant.parse("2026-04-05T18:00:00Z"),
            Map.of()
        );
        ArchitectureIndexDocument document = new ArchitectureIndexDocument(
            "1.3.0",
            "0.1.0-SNAPSHOT",
            new RunMetadata(
                Instant.parse("2026-04-05T18:00:00Z"),
                Instant.parse("2026-04-05T18:00:05Z"),
                RunOutcome.SUCCESS,
                List.of(),
                Map.of()
            ),
            source,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            new CompletenessMetadata(CompletenessStatus.COMPLETE, 0, 0, 0, List.of(), List.of()),
            Map.of()
        );
        return new IndexRunResult(document, acquiredRoot, acquiredRoot.resolveSibling("out.json"), Map.of(), temporaryWorkspace);
    }

    private static final class EndToEndWorkerModeService extends WorkerModeService {
        private final IndexRunResult runResult;

        private EndToEndWorkerModeService(Path workerWorkspaceDirectory, IndexRunResult runResult) {
            super(workerWorkspaceDirectory);
            this.runResult = runResult;
        }

        @Override
        protected IndexRunResult runIndexer(WorkerJobRequest request) {
            Path outputPath = Path.of(request.outputPath());
            try {
                Files.createDirectories(outputPath.getParent());
                ArchitectureIrJson.write(runResult.document(), outputPath);
                Files.writeString(
                    outputPath.resolveSibling(outputPath.getFileName().toString().replace(".json", ".manifest.json")),
                    "{\n  \"status\" : \"SUCCESS\"\n}"
                );
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to write end-to-end test output", exception);
            }
            return runResult;
        }
    }
}
