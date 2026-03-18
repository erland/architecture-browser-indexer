package info.isaksson.erland.architecturebrowser.indexer.ir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.architecturebrowser.indexer.ir.json.ArchitectureIrJson;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFormatSchemaExamplesContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path SCHEMA_DIR = Path.of("docs/export-format/schema");
    private static final Path EXAMPLE_DIR = Path.of("src/test/resources/export-contract");
    private static final Path REAL_FIXTURE_DIR = Path.of("src/test/resources/fixtures/ir");

    @Test
    void schemaFilesAreReadableJsonAndContainExpectedTopLevelDefinitions() throws IOException {
        List<Path> schemaFiles;
        try (var paths = Files.list(SCHEMA_DIR)) {
            schemaFiles = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        assertFalse(schemaFiles.isEmpty(), "expected export schema files");

        for (Path schemaFile : schemaFiles) {
            JsonNode schema = MAPPER.readTree(Files.readString(schemaFile));
            assertTrue(schema.isObject(), () -> schemaFile.getFileName() + " must contain a JSON object");
            assertTrue(schema.has("$schema"), () -> schemaFile.getFileName() + " missing $schema");
            assertTrue(schema.has("title"), () -> schemaFile.getFileName() + " missing title");
        }

        JsonNode topLevelSchema = MAPPER.readTree(Files.readString(SCHEMA_DIR.resolve("architecture-index-document.schema.json")));
        assertTrue(topLevelSchema.has("required"), "top-level schema should declare required properties");
        Set<String> required = readTextArray(topLevelSchema.get("required"));
        assertTrue(required.containsAll(Set.of(
            "schemaVersion",
            "indexerVersion",
            "runMetadata",
            "source",
            "scopes",
            "entities",
            "relationships",
            "diagnostics",
            "completeness",
            "metadata"
        )), "top-level schema should cover the stable core fields");
    }

    @Test
    void curatedExamplesDeserializeValidateAndSatisfySchemaDeclaredCoreFields() throws IOException {
        JsonNode topLevelSchema = MAPPER.readTree(Files.readString(SCHEMA_DIR.resolve("architecture-index-document.schema.json")));
        Set<String> requiredTopLevel = readTextArray(topLevelSchema.get("required"));
        Set<String> requiredEntityFields = requiredFields("entity.schema.json");
        Set<String> requiredRelationshipFields = requiredFields("relationship.schema.json");
        Set<String> requiredScopeFields = requiredFields("scope.schema.json");
        Set<String> requiredDiagnosticFields = requiredFields("diagnostic.schema.json");
        Set<String> requiredCompletenessFields = requiredFields("completeness.schema.json");
        Set<String> requiredViewpointFields = requiredFields("viewpoint.schema.json");

        List<Path> examples;
        try (var paths = Files.list(EXAMPLE_DIR)) {
            examples = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        assertFalse(examples.isEmpty(), "expected curated export examples");

        for (Path example : examples) {
            JsonNode node = MAPPER.readTree(Files.readString(example));
            assertContainsFields(node, requiredTopLevel, example.getFileName() + " missing top-level core fields");
            assertArrayItemsContainRequiredFields(node.path("scopes"), requiredScopeFields, example.getFileName() + " scope");
            assertArrayItemsContainRequiredFields(node.path("entities"), requiredEntityFields, example.getFileName() + " entity");
            assertArrayItemsContainRequiredFields(node.path("relationships"), requiredRelationshipFields, example.getFileName() + " relationship");
            if (node.has("viewpoints")) {
                assertArrayItemsContainRequiredFields(node.path("viewpoints"), requiredViewpointFields, example.getFileName() + " viewpoint");
            }
            assertArrayItemsContainRequiredFields(node.path("diagnostics"), requiredDiagnosticFields, example.getFileName() + " diagnostic");
            assertContainsFields(node.path("completeness"), requiredCompletenessFields, example.getFileName() + " completeness");

            ArchitectureIndexDocument document = ArchitectureIrJson.fromJson(Files.readString(example));
            ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
            assertTrue(validation.isValid(), () -> example.getFileName() + " should validate: " + validation.messages());
        }
    }

    @Test
    void checkedInRealIrFixturesDeserializeValidateAndSatisfySchemaDeclaredCoreFields() throws IOException {
        JsonNode topLevelSchema = MAPPER.readTree(Files.readString(SCHEMA_DIR.resolve("architecture-index-document.schema.json")));
        Set<String> requiredTopLevel = readTextArray(topLevelSchema.get("required"));

        List<Path> fixtures;
        try (var paths = Files.list(REAL_FIXTURE_DIR)) {
            fixtures = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        assertFalse(fixtures.isEmpty(), "expected checked-in IR fixtures");

        for (Path fixture : fixtures) {
            JsonNode node = MAPPER.readTree(Files.readString(fixture));
            assertContainsFields(node, requiredTopLevel, fixture.getFileName() + " missing top-level core fields");

            ArchitectureIndexDocument document = ArchitectureIrJson.read(fixture);
            ArchitectureIrValidator.ValidationResult validation = ArchitectureIrValidator.validate(document);
            assertTrue(validation.isValid(), () -> fixture.getFileName() + " should validate: " + validation.messages());
        }
    }

    private static Set<String> requiredFields(String schemaFileName) throws IOException {
        JsonNode schema = MAPPER.readTree(Files.readString(SCHEMA_DIR.resolve(schemaFileName)));
        return readTextArray(schema.get("required"));
    }

    private static Set<String> readTextArray(JsonNode arrayNode) {
        assertTrue(arrayNode != null && arrayNode.isArray(), "expected JSON array of required field names");
        List<String> values = new ArrayList<>();
        arrayNode.forEach(node -> values.add(node.asText()));
        return Set.copyOf(values);
    }

    private static void assertContainsFields(JsonNode objectNode, Set<String> requiredFields, String messagePrefix) {
        assertTrue(objectNode.isObject(), () -> messagePrefix + " should be a JSON object");
        for (String field : requiredFields) {
            assertTrue(objectNode.has(field), () -> messagePrefix + ": missing field '" + field + "'");
        }
    }

    private static void assertArrayItemsContainRequiredFields(JsonNode arrayNode, Set<String> requiredFields, String messagePrefix) {
        assertTrue(arrayNode.isArray(), () -> messagePrefix + " should be a JSON array");
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode item = arrayNode.get(i);
            assertContainsFields(item, requiredFields, messagePrefix + "[" + i + "]");
        }
    }
}
