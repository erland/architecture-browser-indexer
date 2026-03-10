package info.isaksson.erland.architecturebrowser.indexer.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileInventoryScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void scansDeterministicallyAndDetectsLanguagesAndMarkers() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.createDirectories(tempDir.resolve("src/web"));
        Files.writeString(tempDir.resolve("pom.xml"), "<project><artifactId>x</artifactId><dependency>spring-boot</dependency></project>");
        Files.writeString(tempDir.resolve("src/main/java/com/example/Demo.java"), "class Demo {}\n");
        Files.writeString(tempDir.resolve("src/web/package.json"), "{\"dependencies\":{\"react\":\"18.0.0\",\"typescript\":\"5.0.0\"}}\n");
        Files.writeString(tempDir.resolve("src/web/app.tsx"), "export const App = () => null;\n");

        FileInventoryScanner scanner = new FileInventoryScanner();
        FileInventory first = scanner.scan(tempDir);
        FileInventory second = scanner.scan(tempDir);

        assertEquals(first.entries(), second.entries());
        assertTrue(first.detectedLanguages().contains("java"));
        assertTrue(first.detectedLanguages().contains("typescript"));
        assertTrue(first.detectedTechnologyMarkers().contains("spring"));
        assertTrue(first.detectedTechnologyMarkers().contains("react"));
        assertEquals(List.of(
            "pom.xml",
            "src/main/java/com/example/Demo.java",
            "src/web/app.tsx",
            "src/web/package.json"
        ), first.entries().stream().map(FileInventoryEntry::relativePath).toList());
    }

    @Test
    void marksIgnoredFilesWithoutFailingScan() throws Exception {
        Files.createDirectories(tempDir.resolve("node_modules/lib"));
        Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(tempDir.resolve("node_modules/lib/index.js"), "module.exports = {};\n");
        Files.write(tempDir.resolve("docs/blob.bin"), new byte[]{0, 1, 2, 3});
        Files.writeString(tempDir.resolve("application.properties"), "quarkus.http.port=8080\n");

        FileInventory inventory = new FileInventoryScanner().scan(tempDir);

        assertEquals(3, inventory.totalFiles());
        assertEquals(1, inventory.ignoredFiles());
        assertTrue(inventory.entries().stream().anyMatch(entry -> entry.relativePath().equals("node_modules/lib/index.js") && entry.ignored()));
        assertTrue(inventory.detectedTechnologyMarkers().contains("quarkus"));
    }
}
