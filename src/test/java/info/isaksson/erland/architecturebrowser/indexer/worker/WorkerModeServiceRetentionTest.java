package info.isaksson.erland.architecturebrowser.indexer.worker;

import info.isaksson.erland.architecturebrowser.indexer.application.IndexRunResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobRequest;
import info.isaksson.erland.architecturebrowser.indexer.worker.model.WorkerJobResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerModeServiceRetentionTest {

    @Test
    void retainsGitWorkspaceUnderWorkerRetentionDirectory() throws Exception {
        Path workspace = Files.createTempDirectory("ab-worker-retain-git");
        Path gitTempWorkspace = Files.createTempDirectory("ab-git-temp");
        Path acquiredRoot = gitTempWorkspace.resolve("repo");
        Files.createDirectories(acquiredRoot.resolve("src"));
        Files.writeString(acquiredRoot.resolve("src/App.tsx"), "export const App = () => null;\n");

        StubWorkerModeService service = new StubWorkerModeService(workspace, indexRunResult(acquiredRoot, true, "git", "repo-1", "rev-123"));
        WorkerJobResult result = service.runJob(new WorkerJobRequest(
            "job-1",
            "repo-1",
            null,
            "https://github.com/example/repo.git",
            "main",
            workspace.resolve("out.json").toString(),
            null,
            null
        ), workspace.resolve("result.json"));

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceAccess = (Map<String, Object>) result.summary().get("sourceAccess");
        assertNotNull(sourceAccess);
        assertEquals("SOURCE_HANDLE", sourceAccess.get("lookupKeyKind"));
        assertEquals("RETAINED_GIT_CHECKOUT", sourceAccess.get("retainedRootKind"));
        assertEquals("GIT", sourceAccess.get("acquisitionType"));
        assertEquals("repo-1", sourceAccess.get("repositoryId"));
        assertEquals("rev-123", sourceAccess.get("sourceRevision"));
        assertEquals("ttl-7d", sourceAccess.get("retentionPolicy"));

        String sourceHandle = (String) sourceAccess.get("sourceHandle");
        assertNotNull(sourceHandle);
        Path retainedRoot = Path.of((String) result.summary().get("retainedSourceRoot"));
        assertTrue(Files.exists(retainedRoot.resolve("src/App.tsx")));
        assertFalse(Files.exists(gitTempWorkspace));
        assertTrue(Files.exists(workspace.resolve("source-retention/handles").resolve(sourceHandle + ".json")));
    }

    @Test
    void recordsLocalPathHandleWithoutCopyingSourceTree() throws Exception {
        Path workspace = Files.createTempDirectory("ab-worker-retain-local");
        Path localRoot = Files.createTempDirectory("ab-local-root");
        Files.createDirectories(localRoot.resolve("src/main/java"));
        Files.writeString(localRoot.resolve("src/main/java/App.java"), "class App {}\n");

        StubWorkerModeService service = new StubWorkerModeService(workspace, indexRunResult(localRoot, false, "local-path", "repo-2", "rev-456"));
        WorkerJobResult result = service.runJob(new WorkerJobRequest(
            "job-2",
            "repo-2",
            localRoot.toString(),
            null,
            null,
            workspace.resolve("out.json").toString(),
            null,
            null
        ), workspace.resolve("result.json"));

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceAccess = (Map<String, Object>) result.summary().get("sourceAccess");
        assertNotNull(sourceAccess);
        assertEquals("LOCAL_PATH_REFERENCE", sourceAccess.get("retainedRootKind"));
        assertEquals("LOCAL_PATH", sourceAccess.get("acquisitionType"));
        assertEquals("local-path-reference", sourceAccess.get("retentionPolicy"));
        assertNull(sourceAccess.get("expiresAt"));
        assertEquals(localRoot.toAbsolutePath().normalize().toString(), result.summary().get("retainedSourceRoot"));
        assertTrue(Files.exists(localRoot.resolve("src/main/java/App.java")));
    }

    private static IndexRunResult indexRunResult(Path acquiredRoot, boolean temporaryWorkspace, String acquisitionType, String repositoryId, String revision) {
        RepositorySource source = new RepositorySource(
            repositoryId,
            acquisitionType,
            acquiredRoot.toString(),
            acquisitionType.equals("git") ? "https://github.com/example/repo.git" : null,
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

    private static final class StubWorkerModeService extends WorkerModeService {
        private final IndexRunResult runResult;

        private StubWorkerModeService(Path workerWorkspaceDirectory, IndexRunResult runResult) {
            super(workerWorkspaceDirectory);
            this.runResult = runResult;
        }

        @Override
        protected IndexRunResult runIndexer(WorkerJobRequest request) {
            return runResult;
        }
    }
}
