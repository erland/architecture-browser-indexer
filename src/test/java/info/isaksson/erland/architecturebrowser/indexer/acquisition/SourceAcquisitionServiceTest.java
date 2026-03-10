package info.isaksson.erland.architecturebrowser.indexer.acquisition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAcquisitionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void acquiresLocalPathAndCapturesGitMetadataWhenAvailable() throws Exception {
        Path repository = tempDir.resolve("sample-repo");
        Files.createDirectories(repository);
        Files.writeString(repository.resolve("pom.xml"), "<project></project>");

        git(repository, "init");
        git(repository, "config", "user.email", "test@example.com");
        git(repository, "config", "user.name", "Test User");
        git(repository, "add", ".");
        git(repository, "commit", "-m", "initial");

        SourceAcquisitionService service = new SourceAcquisitionService();
        AcquisitionResult result = service.acquire(new AcquisitionRequest(null, repository, null, null, null));

        assertEquals(repository.toAbsolutePath().normalize(), result.acquiredRoot());
        assertEquals("local-path", result.repositorySource().acquisitionType());
        assertNotNull(result.repositorySource().revision());
        assertTrue((Boolean) result.repositorySource().metadata().get("gitWorkingTree"));
    }

    @Test
    void clonesLocalGitRepositoryViaGitAcquisition() throws Exception {
        Path origin = tempDir.resolve("origin");
        Files.createDirectories(origin);
        Files.writeString(origin.resolve("package.json"), "{\"dependencies\":{\"react\":\"1.0.0\"}}");

        git(origin, "init");
        git(origin, "config", "user.email", "test@example.com");
        git(origin, "config", "user.name", "Test User");
        git(origin, "add", ".");
        git(origin, "commit", "-m", "initial");

        SourceAcquisitionService service = new SourceAcquisitionService();
        AcquisitionResult result = service.acquire(new AcquisitionRequest("sample", null, origin.toString(), null, tempDir));

        assertEquals("git", result.repositorySource().acquisitionType());
        assertEquals("sample", result.repositorySource().repositoryId());
        assertTrue(Files.exists(result.acquiredRoot().resolve("package.json")));
        assertNotNull(result.repositorySource().revision());
    }

    private static void git(Path directory, String... command) throws IOException, InterruptedException {
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(command));
        Process process = new ProcessBuilder(cmd).directory(directory.toFile()).redirectErrorStream(true).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Git command failed: " + String.join(" ", cmd) + "\n" + new String(process.getInputStream().readAllBytes()));
        }
    }
}
