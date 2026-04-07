package info.isaksson.erland.architecturebrowser.indexer.publish;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotSourceFileReaderTest {

    @Test
    void readsReferencedTextFilesFromIndexedSourceRoot() throws Exception {
        Path root = Files.createTempDirectory("snapshot-source-reader");
        Files.createDirectories(root.resolve("src/main/java/demo"));
        Files.writeString(root.resolve("src/main/java/demo/App.java"), "package demo;\nclass App {}\n");

        SnapshotSourceFileReadResult result = new SnapshotSourceFileReader()
            .readReferencedTextFiles(root, List.of("src/main/java/demo/App.java"));

        assertEquals(1, result.files().size());
        assertEquals("src/main/java/demo/App.java", result.files().get(0).relativePath());
        assertEquals("package demo;\nclass App {}\n", result.files().get(0).textContent());
        assertEquals("java", result.files().get(0).language());
        assertEquals("text/x-java-source", result.files().get(0).contentType());
        assertEquals(2, result.files().get(0).totalLineCount());
        assertEquals(List.of(), result.skippedRelativePaths());
        assertEquals(List.of(), result.skippedFiles());
    }

    @Test
    void skipsMissingOrUnsafeReferencedFilesWithReasons() throws Exception {
        Path root = Files.createTempDirectory("snapshot-source-reader-skip");
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/ok.txt"), "hello\n");
        Files.writeString(root.resolve("src/app.min.js"), "const x=1;");

        SnapshotSourceFileReadResult result = new SnapshotSourceFileReader()
            .readReferencedTextFiles(root, List.of("src/ok.txt", "../escape.txt", "src/missing.txt", "src/app.min.js"));

        assertEquals(1, result.files().size());
        assertEquals(List.of("../escape.txt", "src/missing.txt", "src/app.min.js"), result.skippedRelativePaths());
        assertEquals(3, result.skippedFiles().size());
        assertEquals("invalid_path", result.skippedFiles().get(0).reason());
        assertEquals("missing_file", result.skippedFiles().get(1).reason());
        assertEquals("minified_asset", result.skippedFiles().get(2).reason());
    }

    @Test
    void skipsFilesBeyondConfiguredReferencedFileLimit() throws Exception {
        Path root = Files.createTempDirectory("snapshot-source-reader-limit");
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/one.txt"), "one\n");
        Files.writeString(root.resolve("src/two.txt"), "two\n");

        SnapshotSourceFileReadResult result = new SnapshotSourceFileReader(1024L * 1024L, 1)
            .readReferencedTextFiles(root, List.of("src/one.txt", "src/two.txt"));

        assertEquals(1, result.files().size());
        assertEquals(List.of("src/two.txt"), result.skippedRelativePaths());
        assertEquals("referenced_file_limit_exceeded", result.skippedFiles().get(0).reason());
    }

    @Test
    void rejectsBinaryFilesWhenReadDirectly() throws Exception {
        Path root = Files.createTempDirectory("snapshot-source-reader-bin");
        Files.createDirectories(root.resolve("src"));
        Files.write(root.resolve("src/data.bin"), new byte[] {1, 2, 0, 4});

        SnapshotSourceFileReader reader = new SnapshotSourceFileReader();
        assertThrows(IllegalArgumentException.class, () -> reader.readTextFile(root, "src/data.bin"));
    }
}
