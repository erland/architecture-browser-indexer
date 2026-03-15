package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaBackendFrameworkBaselineRegressionTest {

    @Test
    void currentJavaBackendFixtureKeepsStructuralTypesAndDependenciesVisible() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        // Keep the baseline focused on architect-facing visibility rather than on every
        // raw observed Java type being exported as a standalone entity. The important
        // contract for this phase is that the key backend types remain visible through
        // dependency views and supporting evidence, while dedicated JAX-RS/JPA/CDI/write
        // semantics are still intentionally absent.
        // Keep this baseline resilient: we only lock that the current pipeline still
        // exposes useful Java backend structure through dependency/evidence views,
        // without assuming specific interpreted entity promotion before phase 1.

        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertFalse(typeDependencies.isEmpty());

        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        assertFalse(packageDependencies.isEmpty());

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertFalse(evidenceDependencies.isEmpty());

        assertTrue(evidenceDependencies.stream().anyMatch(dep ->
            "src/main/java/com/example/orders/api/OrderResource.java".equals(dep.get("sourceName"))
        ));
        assertTrue(evidenceDependencies.stream().anyMatch(dep ->
            "src/main/java/com/example/orders/domain/OrderEntity.java".equals(dep.get("sourceName"))
        ));
        assertTrue(evidenceDependencies.stream().anyMatch(dep ->
            "src/main/java/com/example/orders/events/OrderCreatedObserver.java".equals(dep.get("sourceName"))
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        @SuppressWarnings("unchecked")
        List<String> recommendedEntryPoints = (List<String>) dependencyViews.get("recommendedEntryPoints");
        assertNotNull(recommendedEntryPoints);
        assertTrue(recommendedEntryPoints.contains("packageDependencies"));
        assertTrue(recommendedEntryPoints.contains("typeDependencies"));
        assertTrue(recommendedEntryPoints.contains("moduleDependencies"));
        assertTrue(recommendedEntryPoints.contains("evidenceDependencies"));

        // Step 2 introduces first-class observed JAX-RS endpoints. The most stable end-to-end
        // contract at this stage is the presence of architect-facing endpoint entities and expose
        // relationships, not that the resource class itself is exported with a particular metadata
        // shape after interpretation/topology normalization.
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.ENDPOINT && "GET /orders".equals(entity.name())),
            () -> "Expected GET endpoint. Entities=" + summarizeEntities(document));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.ENDPOINT && "POST /orders".equals(entity.name())),
            () -> "Expected POST endpoint. Entities=" + summarizeEntities(document));
        assertTrue(document.relationships().stream().anyMatch(relationship -> relationship.kind().name().equals("EXPOSES") && "GET /orders".equals(relationship.label())),
            () -> "Expected EXPOSES relationship for GET /orders. Relationships=" + summarizeRelationships(document));
        assertFalse(dependencyViews.containsKey("javaBackendBrowserViews"));
    }

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-backend-baseline");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());

        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertFalse(parseBatch.results().isEmpty(), "Expected parsed Java fixture files.");
        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for Java fixture. Results=" + summarizeFailures(parseBatch));
        assertTrue(parseBatch.results().stream().allMatch(JavaBackendFrameworkBaselineRegressionTest::usesRealTreeSitterBackend),
            () -> "Expected real Tree-sitter backend for Java fixture. Results=" + parseBatch.results());

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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dependencyViewList(ArchitectureIndexDocument document, String key) {
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        return (List<Map<String, Object>>) dependencyViews.get(key);
    }

    private static boolean usesRealTreeSitterBackend(SourceParseResult result) {
        return "tree-sitter-jtreesitter".equals(String.valueOf(result.metadata().get("parserBackend")));
    }

    private static String summarizeEntities(ArchitectureIndexDocument document) {
        return document.entities().stream()
            .map(entity -> entity.kind() + ":" + entity.name() + " metadata=" + entity.metadata())
            .toList()
            .toString();
    }

    private static String summarizeRelationships(ArchitectureIndexDocument document) {
        return document.relationships().stream()
            .map(relationship -> relationship.kind() + ":" + relationship.label() + " metadata=" + relationship.metadata())
            .toList()
            .toString();
    }

    private static String summarizeFailures(ParseBatchResult parseBatch) {
        return parseBatch.results().stream()
            .filter(result -> result.status() != ParseStatus.SUCCESS)
            .map(result -> result.request().relativePath() + ":" + result.status() + " metadata=" + result.metadata() + " issues=" + result.issues())
            .toList()
            .toString();
    }
}
