package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetainedSourceCleanupServiceTest {

    @Test
    void prunesExpiredRetainedGitCheckoutAndDeletesRetainedRoot() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-cleanup-expired");
        Path tempWorkspace = Files.createTempDirectory("ab-retained-source-cleanup-temp");
        Path repoRoot = tempWorkspace.resolve("repo");
        Files.createDirectories(repoRoot.resolve("src"));
        Files.writeString(repoRoot.resolve("src/App.ts"), "export const app = true;\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(
            workspace,
            new RetainedSourceRetentionSettings(Duration.ofHours(1))
        );
        RetainedSourceHandleRecord record = registry.save(registry.createRetainedGitCheckout(
            tempWorkspace,
            "repo-1",
            "https://github.com/example/repo.git",
            "main",
            "rev-1"
        ));

        RetainedSourceCleanupReport report = new RetainedSourceCleanupService(registry)
            .pruneExpiredAndInvalid(record.expiresAt().plusSeconds(1));

        assertEquals(1, report.prunedExpiredHandles());
        assertFalse(registry.find(record.sourceHandle()).isPresent());
        assertFalse(Files.exists(workspace.resolve("source-retention/roots").resolve(record.sourceHandle())));
    }

    @Test
    void prunesMissingRootHandleRecord() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-cleanup-missing");
        Path localRoot = Files.createTempDirectory("ab-retained-source-cleanup-local-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord record = registry.save(registry.createLocalPathRecord(localRoot, "repo-2", "rev-2"));
        Files.delete(localRoot.resolve("App.java"));
        Files.delete(localRoot);

        RetainedSourceCleanupReport report = new RetainedSourceCleanupService(registry)
            .pruneExpiredAndInvalid(Instant.parse("2026-04-05T18:00:00Z"));

        assertEquals(1, report.prunedMissingRootHandles());
        assertFalse(registry.find(record.sourceHandle()).isPresent());
        assertFalse(Files.exists(registry.handleRecordPath(record.sourceHandle())));
    }

    @Test
    void keepsActiveHandlesWhenRootExistsAndHandleIsNotExpired() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-cleanup-active");
        Path localRoot = Files.createTempDirectory("ab-retained-source-cleanup-active-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord record = registry.save(registry.createLocalPathRecord(localRoot, "repo-3", "rev-3"));

        RetainedSourceCleanupReport report = new RetainedSourceCleanupService(registry)
            .pruneExpiredAndInvalid(Instant.parse("2026-04-05T18:00:00Z"));

        assertEquals(0, report.prunedExpiredHandles());
        assertEquals(0, report.prunedMissingRootHandles());
        assertTrue(registry.find(record.sourceHandle()).isPresent());
    }
}
