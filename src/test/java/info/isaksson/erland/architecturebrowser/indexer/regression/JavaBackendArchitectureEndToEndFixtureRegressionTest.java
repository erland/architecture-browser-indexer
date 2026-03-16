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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaBackendArchitectureEndToEndFixtureRegressionTest {

    @Test
    void realisticJavaBackendFixtureExportsArchitectFacingSemanticsEndToEnd() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        assertTrue(document.entities().stream().anyMatch(entity ->
                entity.kind().name().equals("ENDPOINT")
                    && "GET /orders".equals(entity.displayName())),
            () -> "Expected GET endpoint entity. Entities=" + describeEntities(document));
        assertTrue(document.entities().stream().anyMatch(entity ->
                entity.kind().name().equals("ENDPOINT")
                    && "POST /orders".equals(entity.displayName())),
            () -> "Expected POST endpoint entity. Entities=" + describeEntities(document));

        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "hasAssociation".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.domain.CustomerEntity".equals(rel.label())),
            () -> "Expected JPA entity association to CustomerEntity. Relationships=" + describeRelationships(document));
        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "embeds".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.domain.AddressValue".equals(rel.label())),
            () -> "Expected JPA embedded value relationship to AddressValue. Relationships=" + describeRelationships(document));

        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "publishesEvent".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.events.OrderCreatedEvent".equals(rel.metadata().get("eventType"))
                    && "createOrder".equals(rel.metadata().get("publisherMethod"))),
            () -> "Expected CDI event publication relationship. Relationships=" + describeRelationships(document));
        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "eventObservedBy".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.events.OrderCreatedEvent".equals(rel.metadata().get("eventType"))
                    && "onOrderCreated".equals(rel.metadata().get("observerMethod"))),
            () -> "Expected synchronous CDI observer relationship. Relationships=" + describeRelationships(document));
        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "eventObservedBy".equals(rel.metadata().get("relationshipType"))
                    && Boolean.TRUE.equals(rel.metadata().get("observerAsync"))
                    && "onOrderCreatedAsync".equals(rel.metadata().get("observerMethod"))),
            () -> "Expected async CDI observer relationship. Relationships=" + describeRelationships(document));

        assertTrue(document.relationships().stream().anyMatch(rel ->
                "type".equals(rel.metadata().get("dependencyView"))
                    && "writePath".equals(rel.metadata().get("relationshipType"))
                    && "persist".equals(rel.metadata().get("writeOperation"))
                    && "save".equals(rel.metadata().get("writerMethod"))
                    && "com.example.orders.domain.OrderEntity".equals(rel.label())),
            () -> "Expected repository write path relationship. Relationships=" + describeRelationships(document));

        Map<String, Object> dependencyViews = dependencyViews(document);
        assertViewContains(dependencyViews, "endpointTypeDependencies", "endpoint");
        assertViewContains(dependencyViews, "observerTypeDependencies", "observer-event");
        assertViewContains(dependencyViews, "writePathTypeDependencies", "write-path");
        assertViewContains(dependencyViews, "entityModelTypeDependencies", "entity-model");

        @SuppressWarnings("unchecked")
        Map<String, Object> javaBrowserViews = (Map<String, Object>) dependencyViews.get("javaBrowserViews");
        assertEquals("javaEndpointGraph", javaBrowserViews.get("defaultViewId"));
        assertTrue(((List<?>) javaBrowserViews.get("availableViews")).containsAll(List.of(
            "javaEndpointGraph",
            "javaEntityModelGraph",
            "javaEventFlowGraph",
            "javaWritePathGraph"
        )), () -> "Expected all Java browser views to be available. javaBrowserViews=" + javaBrowserViews);

        @SuppressWarnings("unchecked")
        Map<String, Object> browserViewCatalog = (Map<String, Object>) dependencyViews.get("browserViewCatalog");
        assertEquals("java", browserViewCatalog.get("defaultFamily"));
        assertTrue(((List<?>) browserViewCatalog.get("availableFamilies")).contains("java"),
            () -> "Expected browser view catalog to expose Java family. browserViewCatalog=" + browserViewCatalog);

        assertTrue(document.entities().stream().anyMatch(entity ->
                "FUNCTION".equals(entity.kind().name())
                    && "save".equals(entity.name())
                    && Boolean.TRUE.equals(entity.metadata().get("writePath"))),
            () -> "Expected write-path method metadata on save(). Entities=" + describeEntities(document));
    }

    @SuppressWarnings("unchecked")
    private static void assertViewContains(Map<String, Object> dependencyViews, String key, String architectureViewKind) {
        List<Map<String, Object>> view = (List<Map<String, Object>>) dependencyViews.get(key);
        assertTrue(view != null && !view.isEmpty(), () -> "Expected non-empty dependency view " + key + ". dependencyViews=" + dependencyViews);
        assertTrue(view.stream().anyMatch(dep -> stringList(dep.get("architectureViewKinds")).contains(architectureViewKind)),
            () -> "Expected architecture view kind " + architectureViewKind + " in " + key + ". view=" + view);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dependencyViews(ArchitectureIndexDocument document) {
        return (Map<String, Object>) document.metadata().get("dependencyViews");
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
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

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-backend-architecture-e2e");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());

        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for Java backend e2e fixture. Results=" + summarizeFailures(parseBatch));
        assertTrue(parseBatch.results().stream().allMatch(JavaBackendArchitectureEndToEndFixtureRegressionTest::usesRealTreeSitterBackend),
            () -> "Expected real Tree-sitter backend for Java backend e2e fixture. Results=" + parseBatch.results());

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatch);
        InterpretationResult interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        TopologyResult topology = new TopologyService().infer(inventory, extraction, interpretation);

        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("fixture", fixtureRoot.toAbsolutePath().toString(), Instant.parse("2026-03-16T12:00:00Z")),
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
