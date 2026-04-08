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
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkerModeServiceRetentionTest {

    @Test
    void deletesTemporaryWorkspaceAfterSuccessfulRun() throws Exception {
        Path workspace = Files.createTempDirectory("ab-worker-cleanup");
        Path acquiredParent = Files.createDirectories(workspace.resolve("temp-job"));
        Path acquiredRoot = Files.createDirectories(acquiredParent.resolve("repo"));
        Files.writeString(acquiredRoot.resolve("App.java"), "class App {}\n");

        WorkerModeService service = new WorkerModeService(workspace) {
            @Override
            protected IndexRunResult runIndexer(WorkerJobRequest request) throws Exception {
                Files.createDirectories(Path.of(request.outputPath()).getParent());
                Files.writeString(Path.of(request.outputPath()), "{}\n");
                Path outputPath = Path.of(request.outputPath());
                return new IndexRunResult(new ArchitectureIndexDocument(
                    info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
                    "0.1.0-SNAPSHOT",
                    new RunMetadata(Instant.now(), Instant.now(), RunOutcome.SUCCESS, List.of(), Map.of()),
                    new RepositorySource("repo-1", "git", acquiredRoot.toString(), "https://example/repo.git", "main", "rev-123", Instant.now(), Map.of()),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    new CompletenessMetadata(CompletenessStatus.COMPLETE, 1, 1, 0, List.of(), List.of()),
                    Map.of()
                ), acquiredRoot, outputPath, Map.of(), true);
            }
        };

        WorkerJobResult result = service.runJob(
            new WorkerJobRequest("job-1", "repo-1", null, "https://example/repo.git", "main", workspace.resolve("out.json").toString(), null, null),
            workspace.resolve("result.json")
        );

        assertEquals("SUCCESS", result.status());
        assertFalse(Files.exists(acquiredParent));
        assertNull(result.summary().get("sourceAccess"));
    }
}
