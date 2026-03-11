package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.publish.ExportBundleWriter;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportContractRegressionTest {

    @Test
    void manifestStillTargetsArchitectureIndexDocumentContract() {
        var document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T14:10:00Z")),
            "0.1.0-SNAPSHOT",
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of()
        );

        var bundle = new ExportBundleWriter().createBundle(document, "0.1.0-SNAPSHOT", "sample.json");
        assertEquals("architecture-index-document", bundle.manifest().contract().payloadType());
        assertEquals("1.0", bundle.manifest().contract().contractVersion());
        assertTrue(bundle.manifest().contract().acceptedTargets().contains("FILE"));
        assertTrue(bundle.manifest().contract().acceptedTargets().contains("HTTP_IMPORT"));
    }
}
