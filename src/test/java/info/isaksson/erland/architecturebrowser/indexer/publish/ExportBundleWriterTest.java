package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportBundleWriterTest {

    @Test
    void writesPayloadAndManifestWithCompatibilityMetadata() throws Exception {
        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T13:00:00Z")),
            "0.1.0-SNAPSHOT",
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of()
        );

        ExportBundleWriter writer = new ExportBundleWriter();
        var bundle = writer.createBundle(document, "0.1.0-SNAPSHOT", "sample-export.json");

        Path tempDir = Files.createTempDirectory("ab-index-export");
        Path output = tempDir.resolve("sample-export.json");
        writer.writeBundle(output, bundle);

        Path manifest = tempDir.resolve("sample-export.manifest.json");
        Path sourceFilesArtifact = tempDir.resolve("sample-export.source-files.json");
        assertTrue(Files.exists(output));
        assertTrue(Files.exists(manifest));
        assertTrue(Files.exists(sourceFilesArtifact));
        assertEquals("architecture-index-document", bundle.manifest().contract().payloadType());
        assertEquals("application/json", bundle.manifest().payloadContentType());
        assertTrue(bundle.manifest().payloadSha256().length() >= 32);
        assertEquals(Boolean.TRUE, bundle.manifest().contract().compatibility().get("compatible"));
        assertEquals(0, bundle.snapshotSourceFiles().files().size());
        assertEquals(0, bundle.snapshotSourceFiles().metadata().get("referencedFileCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceArtifact = (Map<String, Object>) bundle.manifest().metadata().get("snapshotSourceFilesArtifact");
        assertEquals("sample-export.source-files.json", sourceArtifact.get("fileName"));
        assertEquals("snapshot-source-files/v1", sourceArtifact.get("contractVersion"));
    }

    @Test
    void readsReferencedSourceFilesIntoBundleWhenIndexedSourceRootIsAvailable() throws Exception {
        Path sourceRoot = Files.createTempDirectory("ab-index-export-source");
        Files.createDirectories(sourceRoot.resolve("src/main/java/demo"));
        Files.writeString(sourceRoot.resolve("src/main/java/demo/App.java"), "package demo;\npublic class App {}\n", StandardCharsets.UTF_8);

        ArchitectureIndexDocument document = new ArchitectureIndexDocument(
            "1.0",
            "test-indexer",
            new RunMetadata(
                Instant.parse("2026-04-06T10:15:00Z"),
                Instant.parse("2026-04-06T10:16:00Z"),
                RunOutcome.SUCCESS,
                List.of("java"),
                Map.of()
            ),
            new RepositorySource("sample", "local-path", sourceRoot.toString(), null, null, null, Instant.parse("2026-04-06T10:14:00Z"), Map.of()),
            List.of(),
            List.of(new ArchitectureEntity(
                "entity:app",
                EntityKind.CLASS,
                EntityOrigin.OBSERVED,
                "demo.App",
                null,
                null,
                List.of(new SourceReference("src/main/java/demo/App.java", 1, 2, null, Map.of())),
                Map.of()
            )),
            List.of(),
            List.of(),
            new CompletenessMetadata(CompletenessStatus.COMPLETE, 1, 0, 0, List.of(), List.of()),
            Map.of()
        );

        ExportBundleWriter writer = new ExportBundleWriter();
        var bundle = writer.createBundle(document, "0.1.0-SNAPSHOT", "sample-export.json", sourceRoot);

        Path tempDir = Files.createTempDirectory("ab-index-export-source-bundle");
        Path output = tempDir.resolve("sample-export.json");
        writer.writeBundle(output, bundle);

        assertTrue(Files.exists(tempDir.resolve("sample-export.source-files.json")));
        assertEquals(1, bundle.snapshotSourceFiles().files().size());
        assertEquals("src/main/java/demo/App.java", bundle.snapshotSourceFiles().files().get(0).relativePath());
        assertEquals("java", bundle.snapshotSourceFiles().files().get(0).language());
        assertEquals(2, bundle.snapshotSourceFiles().files().get(0).totalLineCount());
        assertEquals("text/x-java-source", bundle.snapshotSourceFiles().files().get(0).contentType());
        assertEquals("package demo;\npublic class App {}\n", bundle.snapshotSourceFiles().files().get(0).textContent());
        assertEquals(List.of(), bundle.snapshotSourceFiles().metadata().get("skippedReferencedFiles"));
        assertEquals(0, bundle.snapshotSourceFiles().metadata().get("skippedReferencedFileCount"));
        assertEquals(1, bundle.snapshotSourceFiles().metadata().get("readReferencedFileCount"));
        assertEquals(SnapshotSourceFileReader.DEFAULT_MAX_REFERENCED_FILES, bundle.snapshotSourceFiles().metadata().get("maxReferencedFiles"));
        assertEquals(SnapshotSourceFileReader.DEFAULT_MAX_FILE_SIZE_BYTES, bundle.snapshotSourceFiles().metadata().get("maxReferencedFileSizeBytes"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceArtifact = (Map<String, Object>) bundle.manifest().metadata().get("snapshotSourceFilesArtifact");
        assertEquals("sample-export.source-files.json", sourceArtifact.get("fileName"));
        assertEquals(1, sourceArtifact.get("fileCount"));
    }


    @Test
    void recordsSkipDetailsForUnsafeOrUnwantedReferencedFiles() throws Exception {
        Path sourceRoot = Files.createTempDirectory("ab-index-export-source-skip");
        Files.createDirectories(sourceRoot.resolve("src/main/resources"));
        Files.writeString(sourceRoot.resolve("src/main/resources/app.min.js"), "const x=1;", StandardCharsets.UTF_8);

        ArchitectureIndexDocument document = new ArchitectureIndexDocument(
            "1.0",
            "test-indexer",
            new RunMetadata(
                Instant.parse("2026-04-06T10:15:00Z"),
                Instant.parse("2026-04-06T10:16:00Z"),
                RunOutcome.SUCCESS,
                List.of("javascript"),
                Map.of()
            ),
            new RepositorySource("sample", "local-path", sourceRoot.toString(), null, null, null, Instant.parse("2026-04-06T10:14:00Z"), Map.of()),
            List.of(),
            List.of(new ArchitectureEntity(
                "entity:minified",
                EntityKind.CONFIG_ARTIFACT,
                EntityOrigin.OBSERVED,
                "app.min.js",
                null,
                null,
                List.of(new SourceReference("src/main/resources/app.min.js", 1, 1, null, Map.of())),
                Map.of()
            )),
            List.of(),
            List.of(),
            new CompletenessMetadata(CompletenessStatus.COMPLETE, 1, 0, 0, List.of(), List.of()),
            Map.of()
        );

        ExportBundleWriter writer = new ExportBundleWriter();
        var bundle = writer.createBundle(document, "0.1.0-SNAPSHOT", "sample-export.json", sourceRoot);

        assertEquals(0, bundle.snapshotSourceFiles().files().size());
        assertEquals(List.of("src/main/resources/app.min.js"), bundle.snapshotSourceFiles().metadata().get("skippedReferencedFiles"));
        assertEquals(1, bundle.snapshotSourceFiles().metadata().get("skippedReferencedFileCount"));
    }

}
