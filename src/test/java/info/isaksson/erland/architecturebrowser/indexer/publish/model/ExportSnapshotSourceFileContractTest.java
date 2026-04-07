package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExportSnapshotSourceFileContractTest {
    @Test
    void normalizesSnapshotSourceFileFields() {
        ExportSnapshotSourceFile file = new ExportSnapshotSourceFile(
            "src\\main\\java\\demo\\App.java",
            " java ",
            42,
            3,
            null,
            "class App {}\n"
        );

        assertEquals("src/main/java/demo/App.java", file.relativePath());
        assertEquals("java", file.language());
        assertEquals("text/plain", file.contentType());
    }

    @Test
    void rejectsEscapingRelativePaths() {
        assertThrows(IllegalArgumentException.class, () -> new ExportSnapshotSourceFile(
            "../etc/passwd",
            "java",
            1,
            1,
            "text/plain",
            "x"
        ));
    }

    @Test
    void exportBundleDefaultsToEmptySnapshotSourceFilesContract() {
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
            RepositorySource.localPath("repo-1", "/tmp/repo", Instant.parse("2026-04-06T10:14:00Z")),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            new CompletenessMetadata(CompletenessStatus.COMPLETE, 1, 1, 0, List.of(), List.of()),
            Map.of()
        );
        ExportManifest manifest = new ExportManifest(
            "export-1",
            Instant.parse("2026-04-06T10:16:00Z"),
            "snapshot.json",
            "application/json",
            10,
            "abc",
            new ExportContract("1", "1", "test", "architecture-index", List.of(), Map.of()),
            Map.of()
        );

        ExportBundle bundle = new ExportBundle(document, manifest, null);

        assertEquals("snapshot-source-files/v1", bundle.snapshotSourceFiles().contractVersion());
        assertEquals(List.of(), bundle.snapshotSourceFiles().files());
    }
}
