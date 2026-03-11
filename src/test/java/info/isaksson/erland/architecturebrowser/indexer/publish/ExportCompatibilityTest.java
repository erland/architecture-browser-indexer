package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportContract;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportCompatibilityTest {

    @Test
    void reportsIncompatibleWhenSchemaOrPayloadTypeMismatch() {
        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T13:05:00Z")),
            "0.1.0-SNAPSHOT",
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of()
        );

        ExportContract contract = new ExportContract(
            "1.0",
            "different-schema",
            "test-producer",
            "wrong-payload",
            List.of("FILE"),
            Map.of()
        );

        Map<String, Object> compatibility = ExportCompatibility.evaluate(contract, document);
        assertEquals(Boolean.FALSE, compatibility.get("compatible"));
        assertEquals(Boolean.FALSE, compatibility.get("schemaMatches"));
        assertEquals(Boolean.FALSE, compatibility.get("payloadTypeMatches"));
    }
}
