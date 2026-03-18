package info.isaksson.erland.architecturebrowser.indexer.ir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrContractEvolutionBaselineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path SCHEMA_DIR = Path.of("docs/export-format/schema");

    @Test
    void currentSchemaVersionRemainsPinnedUntilStableContractFieldsActuallyChange() {
        assertEquals("1.0.0", ArchitectureIrVersions.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void stableContractSchemasAreStrictSoFutureStableFieldAdditionsMustBeReviewedExplicitly() throws IOException {
        JsonNode documentSchema = readSchema("architecture-index-document.schema.json");
        JsonNode entitySchema = readSchema("entity.schema.json");
        JsonNode relationshipSchema = readSchema("relationship.schema.json");

        assertFalse(documentSchema.path("additionalProperties").asBoolean(true));
        assertFalse(entitySchema.path("additionalProperties").asBoolean(true));
        assertFalse(relationshipSchema.path("additionalProperties").asBoolean(true));
    }

    @Test
    void enrichedMetadataRemainsTheSafeAdditiveExtensionAreaDuringBaselinePhase() throws IOException {
        JsonNode documentSchema = readSchema("architecture-index-document.schema.json");
        JsonNode metadataSchema = documentSchema.path("properties").path("metadata");

        assertTrue(metadataSchema.path("additionalProperties").asBoolean(false));
    }

    private static JsonNode readSchema(String name) throws IOException {
        return MAPPER.readTree(Files.readString(SCHEMA_DIR.resolve(name)));
    }
}
