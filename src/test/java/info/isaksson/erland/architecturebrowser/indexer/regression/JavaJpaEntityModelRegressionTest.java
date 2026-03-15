package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParserRegistry;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterConfiguration;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParserRegistryFactory;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;
import info.isaksson.erland.architecturebrowser.indexer.scan.InventoryScanOptions;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaJpaEntityModelRegressionTest {

    @Test
    void exportsJpaEntityModelMetadataAndRelationships() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        ArchitectureEntity orderEntity = entityByQualifiedName(document, "com.example.orders.domain.OrderEntity");
        ArchitectureEntity customerEntity = entityByQualifiedName(document, "com.example.orders.domain.CustomerEntity");
        ArchitectureEntity addressValue = entityByQualifiedName(document, "com.example.orders.domain.AddressValue");

        assertEquals("com.example.orders.domain.OrderEntity", orderEntity.metadata().get("qualifiedName"));
        assertEquals("com.example.orders.domain.AddressValue", addressValue.metadata().get("qualifiedName"));

        assertTrue(document.entities().stream().anyMatch(entity -> "shippingAddress".equals(entity.name())),
            () -> "Expected shippingAddress field entity. Entities=" + describeEntities(document));
        assertTrue(document.entities().stream().anyMatch(entity -> "id".equals(entity.name())),
            () -> "Expected id field entity. Entities=" + describeEntities(document));
        assertTrue(document.entities().stream().anyMatch(entity -> "version".equals(entity.name())),
            () -> "Expected version field entity. Entities=" + describeEntities(document));

        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "hasAssociation".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.domain.CustomerEntity".equals(rel.label())),
            () -> "Expected JPA association to CustomerEntity. Relationships=" + describeRelationships(document));
        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "embeds".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.domain.AddressValue".equals(rel.label())),
            () -> "Expected JPA embedded-value relationship to AddressValue. Relationships=" + describeRelationships(document));
    }


    private static String describeEntities(ArchitectureIndexDocument document) {
        return document.entities().stream()
            .map(entity -> entity.kind() + ":" + entity.name() + " metadata=" + entity.metadata())
            .toList()
            .toString();
    }

    private static String describeRelationships(ArchitectureIndexDocument document) {
        return document.relationships().stream()
            .map(rel -> rel.kind() + ":" + rel.fromEntityId() + "->" + rel.toEntityId() + " label=" + rel.label() + " metadata=" + rel.metadata())
            .toList()
            .toString();
    }

    private static ArchitectureEntity entityByQualifiedName(ArchitectureIndexDocument document, String qualifiedName) {
        return document.entities().stream()
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-jpa-entity-model");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());

        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for JPA fixture. Results=" + summarizeFailures(parseBatch));
        assertTrue(parseBatch.results().stream().allMatch(JavaJpaEntityModelRegressionTest::usesRealTreeSitterBackend),
            () -> "Expected real Tree-sitter backend for JPA fixture. Results=" + parseBatch.results());

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatch);
        InterpretationResult interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        TopologyResult topology = new TopologyService().infer(inventory, extraction, interpretation);

        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("fixture", fixtureRoot.toAbsolutePath().toString(), Instant.parse("2026-03-15T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatch,
            extraction,
            interpretation,
            topology
        );
    }

    private static boolean usesRealTreeSitterBackend(SourceParseResult result) {
        return "tree-sitter-jtreesitter".equals(String.valueOf(result.metadata().get("parserBackend")));
    }

    private static String summarizeFailures(ParseBatchResult parseBatch) {
        return parseBatch.results().stream()
            .filter(result -> result.status() != ParseStatus.SUCCESS)
            .map(result -> result.request().relativePath() + ":" + result.status() + " metadata=" + result.metadata() + " issues=" + result.issues())
            .toList()
            .toString();
    }
}
