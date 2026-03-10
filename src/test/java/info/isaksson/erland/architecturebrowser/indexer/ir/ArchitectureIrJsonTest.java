package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrJsonTest {

    @Test
    void roundTripSerializationPreservesCoreFields() {
        ArchitectureIndexDocument original = ArchitectureIrFactory.createPlaceholderDocument(
            RepositorySource.localPath("sample-repo", "/tmp/sample-repo", Instant.parse("2026-03-10T12:00:00Z")),
            "0.1.0-SNAPSHOT"
        );

        String json = ArchitectureIrJson.toJson(original);
        ArchitectureIndexDocument parsed = ArchitectureIrJson.fromJson(json);

        assertEquals(original.schemaVersion(), parsed.schemaVersion());
        assertEquals(original.indexerVersion(), parsed.indexerVersion());
        assertEquals(original.runMetadata().outcome(), parsed.runMetadata().outcome());
        assertEquals(original.source().repositoryId(), parsed.source().repositoryId());
        assertEquals(original.entities().size(), parsed.entities().size());
        assertEquals(original.relationships().size(), parsed.relationships().size());
        assertEquals(original.completeness().status(), parsed.completeness().status());
    }

    @Test
    void fixtureDocumentsValidateAndContainVersionMetadata() throws IOException {
        Path fixtureDir = Path.of("src/test/resources/fixtures/ir");
        try (var paths = Files.list(fixtureDir)) {
            paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                ArchitectureIndexDocument document = ArchitectureIrJson.read(path);
                ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
                assertTrue(validation.isValid(), () -> path.getFileName() + " should validate: " + validation.messages());
                assertFalse(document.schemaVersion().isBlank(), () -> path.getFileName() + " missing schemaVersion");
                assertFalse(document.indexerVersion().isBlank(), () -> path.getFileName() + " missing indexerVersion");
            });
        }
    }

    @Test
    void partialFixtureRetainsPartialCompleteness() {
        ArchitectureIndexDocument document = ArchitectureIrJson.read(Path.of("src/test/resources/fixtures/ir/partial-result.json"));
        assertEquals(CompletenessStatus.PARTIAL, document.completeness().status());
        assertEquals(1, document.diagnostics().size());
        assertEquals("typescript.parse-error", document.diagnostics().get(0).code());
    }
}
