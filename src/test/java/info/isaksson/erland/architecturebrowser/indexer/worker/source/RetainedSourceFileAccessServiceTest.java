package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetainedSourceFileAccessServiceTest {

    @Test
    void resolvesRepositoryRelativeTextFilesWithinRetainedRoot() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-access-workspace");
        Path retainedRoot = Files.createTempDirectory("ab-source-access-root");
        Files.createDirectories(retainedRoot.resolve("src/main/java"));
        Files.writeString(retainedRoot.resolve("src/main/java/App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = registry.save(registry.createLocalPathRecord(retainedRoot, "repo-1", "rev-1"));
        RetainedSourceFileAccessService accessService = new RetainedSourceFileAccessService(registry);

        RetainedSourceResolvedFile resolved = accessService.resolveActiveTextFile(saved.sourceHandle(), "src/main/java/App.java");

        assertEquals("src/main/java/App.java", resolved.relativePath());
        assertEquals(retainedRoot.resolve("src/main/java/App.java").toRealPath(), resolved.resolvedFile());
        assertEquals("class App {}\n", accessService.readUtf8Text(resolved));
    }

    @Test
    void rejectsPathTraversalOutsideRetainedRoot() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-access-traversal-workspace");
        Path retainedRoot = Files.createTempDirectory("ab-source-access-traversal-root");
        Files.writeString(retainedRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = registry.save(registry.createLocalPathRecord(retainedRoot, "repo-2", "rev-2"));
        RetainedSourceFileAccessService accessService = new RetainedSourceFileAccessService(registry);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            accessService.resolveActiveTextFile(saved.sourceHandle(), "../secrets.txt")
        );
        assertTrue(exception.getMessage().contains("escapes") || exception.getMessage().contains("parent traversal"));
    }

    @Test
    void rejectsSymlinkEscapesOutsideRetainedRoot() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-access-symlink-workspace");
        Path retainedRoot = Files.createTempDirectory("ab-source-access-symlink-root");
        Path outside = Files.createTempDirectory("ab-source-access-symlink-outside");
        Files.writeString(outside.resolve("Secret.java"), "class Secret {}\n");
        Files.createSymbolicLink(retainedRoot.resolve("Linked.java"), outside.resolve("Secret.java"));

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = registry.save(registry.createLocalPathRecord(retainedRoot, "repo-3", "rev-3"));
        RetainedSourceFileAccessService accessService = new RetainedSourceFileAccessService(registry);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            accessService.resolveActiveTextFile(saved.sourceHandle(), "Linked.java")
        );
        assertTrue(exception.getMessage().contains("regular file") || exception.getMessage().contains("outside"));
    }

    @Test
    void rejectsLikelyBinaryFiles() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-access-binary-workspace");
        Path retainedRoot = Files.createTempDirectory("ab-source-access-binary-root");
        Files.write(retainedRoot.resolve("image.bin"), new byte[] {0x01, 0x02, 0x00, 0x04});

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = registry.save(registry.createLocalPathRecord(retainedRoot, "repo-4", "rev-4"));
        RetainedSourceFileAccessService accessService = new RetainedSourceFileAccessService(registry);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            accessService.resolveActiveTextFile(saved.sourceHandle(), "image.bin")
        );
        assertTrue(exception.getMessage().contains("text file"));
    }

    @Test
    void rejectsFilesThatExceedConfiguredMaxSize() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-access-size-workspace");
        Path retainedRoot = Files.createTempDirectory("ab-source-access-size-root");
        Files.writeString(retainedRoot.resolve("Big.java"), "12345678901");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord saved = registry.save(registry.createLocalPathRecord(retainedRoot, "repo-5", "rev-5"));
        RetainedSourceFileAccessService accessService = new RetainedSourceFileAccessService(registry, 10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            accessService.resolveActiveTextFile(saved.sourceHandle(), "Big.java")
        );
        assertTrue(exception.getMessage().contains("max allowed size"));
    }

    @Test
    void rejectsExpiredHandlesBeforeResolvingFiles() throws Exception {
        Path workspace = Files.createTempDirectory("ab-source-access-expired-workspace");
        Path retainedRoot = Files.createTempDirectory("ab-source-access-expired-root");
        Files.writeString(retainedRoot.resolve("App.java"), "class App {}\n");

        RetainedSourceHandleRegistryService registry = new RetainedSourceHandleRegistryService(workspace);
        RetainedSourceHandleRecord expiring = new RetainedSourceHandleRecord(
            "src_EXPIRED_FILE_ACCESS",
            retainedRoot,
            RetainedSourceSupport.RETAINED_ROOT_KIND_LOCAL_PATH_REFERENCE,
            RetainedSourceSupport.ACQUISITION_TYPE_LOCAL_PATH,
            "repo-6",
            null,
            null,
            "rev-6",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-02T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"),
            RetainedSourceSupport.RETENTION_POLICY_LOCAL_PATH_REFERENCE
        );
        registry.save(expiring);
        RetainedSourceFileAccessService accessService = new RetainedSourceFileAccessService(registry);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            accessService.resolveActiveTextFile("src_EXPIRED_FILE_ACCESS", "App.java", Instant.parse("2026-04-03T00:00:00Z"))
        );
        assertTrue(exception.getMessage().contains("expired"));
    }
}
