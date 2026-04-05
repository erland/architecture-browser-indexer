package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetainedSourceHandleRegistryServiceTest {

    @Test
    void savesAndReloadsLocalPathRecords() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-registry");
        Path localRoot = Files.createTempDirectory("ab-retained-source-local-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService service = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = service.save(service.createLocalPathRecord(localRoot, "repo-1", "rev-1"));
        RetainedSourceHandleRecord loaded = service.getActive(saved.sourceHandle());

        assertEquals(saved.sourceHandle(), loaded.sourceHandle());
        assertEquals(localRoot.toAbsolutePath().normalize(), loaded.retainedRoot());
        assertEquals("LOCAL_PATH_REFERENCE", loaded.retainedRootKind());
        assertEquals("LOCAL_PATH", loaded.acquisitionType());
        assertEquals("repo-1", loaded.repositoryId());
        assertEquals("rev-1", loaded.sourceRevision());
        assertTrue(Files.exists(service.handleRecordPath(saved.sourceHandle())));
    }

    @Test
    void touchUpdatesLastAccessedAt() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-touch");
        Path localRoot = Files.createTempDirectory("ab-retained-source-touch-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService service = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = service.save(service.createLocalPathRecord(localRoot, "repo-2", "rev-2"));
        Instant touchedAt = Instant.parse("2026-04-05T20:15:00Z");

        RetainedSourceHandleRecord touched = service.touch(saved.sourceHandle(), touchedAt);

        assertEquals(touchedAt, touched.lastAccessedAt());
        assertEquals(touchedAt, service.getRequired(saved.sourceHandle()).lastAccessedAt());
    }

    @Test
    void rejectsExpiredHandleWhenLoadingActiveRecord() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-expired");
        Path localRoot = Files.createTempDirectory("ab-retained-source-expired-root");
        Files.writeString(localRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService service = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord expiring = new RetainedSourceHandleRecord(
            "src_EXPIRED",
            localRoot,
            RetainedSourceSupport.RETAINED_ROOT_KIND_LOCAL_PATH_REFERENCE,
            RetainedSourceSupport.ACQUISITION_TYPE_LOCAL_PATH,
            "repo-3",
            null,
            null,
            "rev-3",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-06T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"),
            RetainedSourceSupport.RETENTION_POLICY_LOCAL_PATH_REFERENCE
        );
        service.save(expiring);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.getActive("src_EXPIRED", Instant.parse("2026-04-07T00:00:00Z"))
        );
        assertTrue(exception.getMessage().contains("expired"));
        List<RetainedSourceHandleRecord> expiredRecords = service.findExpired(Instant.parse("2026-04-07T00:00:00Z"));
        assertEquals(1, expiredRecords.size());
    }


    @Test
    void createRetainedGitCheckoutUsesConfiguredTtl() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-configured-ttl");
        Path tempWorkspace = Files.createTempDirectory("ab-retained-source-configured-ttl-temp");
        Path repoRoot = tempWorkspace.resolve("repo");
        Files.createDirectories(repoRoot.resolve("src"));
        Files.writeString(repoRoot.resolve("src/App.ts"), "export const app = true;\n");

        RetainedSourceHandleRegistryService service = new RetainedSourceHandleRegistryService(
            workspace,
            new RetainedSourceRetentionSettings(java.time.Duration.ofHours(12))
        );
        RetainedSourceHandleRecord record = service.save(service.createRetainedGitCheckout(
            tempWorkspace,
            "repo-ttl",
            "https://github.com/example/repo.git",
            "main",
            "rev-ttl"
        ));

        assertEquals(java.time.Duration.ofHours(12), java.time.Duration.between(record.createdAt(), record.expiresAt()));
    }

    @Test
    void createRetainedGitCheckoutMovesWorkspaceAndDeletesRegistryEntry() throws Exception {
        Path workspace = Files.createTempDirectory("ab-retained-source-git-workspace");
        Path tempWorkspace = Files.createTempDirectory("ab-retained-source-git-temp");
        Path repoRoot = tempWorkspace.resolve("repo");
        Files.createDirectories(repoRoot.resolve("src"));
        Files.writeString(repoRoot.resolve("src/App.ts"), "export const app = true;\n");

        RetainedSourceHandleRegistryService service = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord record = service.save(service.createRetainedGitCheckout(
            tempWorkspace,
            "repo-4",
            "https://github.com/example/repo.git",
            "main",
            "rev-4"
        ));

        assertFalse(Files.exists(tempWorkspace));
        assertTrue(Files.exists(record.retainedRoot().resolve("src/App.ts")));
        assertNotNull(record.expiresAt());
        assertTrue(service.delete(record.sourceHandle()));
        assertFalse(Files.exists(service.handleRecordPath(record.sourceHandle())));
        assertFalse(Files.exists(workspace.resolve("source-retention/roots").resolve(record.sourceHandle())));
    }
}
