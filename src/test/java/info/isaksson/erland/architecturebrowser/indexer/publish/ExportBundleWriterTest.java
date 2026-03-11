package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
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
        assertTrue(Files.exists(output));
        assertTrue(Files.exists(manifest));
        assertEquals("architecture-index-document", bundle.manifest().contract().payloadType());
        assertEquals("application/json", bundle.manifest().payloadContentType());
        assertTrue(bundle.manifest().payloadSha256().length() >= 32);
        assertEquals(Boolean.TRUE, bundle.manifest().contract().compatibility().get("compatible"));
    }
}
