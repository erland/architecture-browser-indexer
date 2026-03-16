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

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFrameworkTopologyRegressionTest {

    @Test
    void backendFrameworkRelationshipsSurviveIntoFrameworkSpecificDependencyViews() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        Map<String, Object> dependencyViews = document.metadata().containsKey("dependencyViews")
            ? (Map<String, Object>) document.metadata().get("dependencyViews")
            : Map.of();

        List<Map<String, Object>> endpointTypeDependencies = dependencyViewList(dependencyViews, "endpointTypeDependencies");
        List<Map<String, Object>> entityModelTypeDependencies = dependencyViewList(dependencyViews, "entityModelTypeDependencies");
        List<Map<String, Object>> observerTypeDependencies = dependencyViewList(dependencyViews, "observerTypeDependencies");
        List<Map<String, Object>> writePathTypeDependencies = dependencyViewList(dependencyViews, "writePathTypeDependencies");

        assertTrue(
            endpointTypeDependencies.stream().anyMatch(dep -> String.valueOf(dep.get("relationshipKind")).contains("EXPOSES")
                || stringList(dep.get("frameworks")).contains("jax-rs")
                || stringList(dep.get("architectureViewKinds")).contains("endpoint"))
                || dependencyViewList(dependencyViews, "endpointModuleDependencies").stream().anyMatch(dep -> stringList(dep.get("architectureViewKinds")).contains("endpoint")),
            () -> "Expected endpoint rollups. dependencyViews=" + dependencyViews);
        assertTrue(
            dependencyViews.containsKey("entityModelTypeDependencies") && dependencyViews.containsKey("entityModelModuleDependencies"),
            () -> "Expected entity-model view buckets to exist even when the baseline fixture has no JPA association graph. dependencyViews=" + dependencyViews);
        assertTrue(observerTypeDependencies.stream().anyMatch(dep -> stringList(dep.get("frameworkRelationships")).stream().anyMatch(v -> List.of("publishesEvent", "observesEvent", "eventObservedBy").contains(v))),
            () -> "Expected observer-event rollups. dependencyViews=" + dependencyViews);
        assertTrue(writePathTypeDependencies.stream().anyMatch(dep -> stringList(dep.get("frameworkRelationships")).contains("writePath")),
            () -> "Expected write-path rollups. dependencyViews=" + dependencyViews);

        Map<String, Object> javaViews = (Map<String, Object>) dependencyViews.get("javaFrameworkArchitectureViews");
        assertTrue(javaViews != null && javaViews.containsKey("endpoints") && javaViews.containsKey("entityModel") && javaViews.containsKey("observerEvents") && javaViews.containsKey("writePaths"),
            () -> "Expected java framework architecture view map. dependencyViews=" + dependencyViews);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dependencyViewList(Map<String, Object> dependencyViews, String key) {
        Object value = dependencyViews.get(key);
        return value instanceof List<?> list ? (List<Map<String, Object>>) value : List.of();
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-backend-baseline");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());
        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for Java topology fixture. Results=" + summarizeFailures(parseBatch));
        assertTrue(parseBatch.results().stream().allMatch(JavaFrameworkTopologyRegressionTest::usesRealTreeSitterBackend),
            () -> "Expected real Tree-sitter backend for Java topology fixture. Results=" + parseBatch.results());

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
