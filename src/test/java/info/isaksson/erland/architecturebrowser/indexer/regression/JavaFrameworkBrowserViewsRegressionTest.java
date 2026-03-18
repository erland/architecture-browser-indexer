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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFrameworkBrowserViewsRegressionTest {

    @Test
    void exportsBrowserFacingJavaFrameworkGraphDescriptors() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        Map<String, Object> dependencyViews = document.metadata().containsKey("dependencyViews")
            ? (Map<String, Object>) document.metadata().get("dependencyViews")
            : Map.of();

        @SuppressWarnings("unchecked")
        Map<String, Object> javaBrowserViews = (Map<String, Object>) dependencyViews.get("javaBrowserViews");
        assertEquals("javaEndpointGraph", javaBrowserViews.get("defaultViewId"));
        assertTrue(((List<?>) javaBrowserViews.get("availableViews")).containsAll(List.of(
            "javaEndpointGraph",
            "javaEventFlowGraph",
            "javaWritePathGraph"
        )), () -> "Expected browser-available Java views. javaBrowserViews=" + javaBrowserViews);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> views = (List<Map<String, Object>>) javaBrowserViews.get("views");
        assertBrowserView(views, "javaEndpointGraph", "jax-rs", "endpointTypeDependencies", "endpointModuleDependencies", "endpoint");
        assertBrowserView(views, "javaEventFlowGraph", "cdi", "observerTypeDependencies", "observerModuleDependencies", "observesEvent");
        assertBrowserView(views, "javaWritePathGraph", "jpa", "writePathTypeDependencies", "writePathModuleDependencies", "writePath");

        Map<String, Object> entityModelView = views.stream()
            .filter(view -> "javaEntityModelGraph".equals(view.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing javaEntityModelGraph. views=" + views));
        assertEquals("entityModelTypeDependencies", entityModelView.get("typeDependencyView"));
        assertEquals("entityModelModuleDependencies", entityModelView.get("moduleDependencyView"));
        assertEquals(Boolean.FALSE, entityModelView.get("available"),
            () -> "Baseline fixture should describe the entity model view even before association edges exist. view=" + entityModelView);

        assertTrue(preferredDependencyViews(document, "api-surface").contains("endpointTypeDependencies"));
        assertTrue(preferredDependencyViews(document, "request-handling").contains("writePathTypeDependencies"));
        assertTrue(preferredDependencyViews(document, "persistence-model").contains("entityModelTypeDependencies"));
        assertTrue(evidenceSources(document, "api-surface").contains("java-browser-views"));
        assertEquals("available", viewpointAvailability(document, "event-flow"));

        @SuppressWarnings("unchecked")
        Map<String, Object> browserViewCatalog = (Map<String, Object>) dependencyViews.get("browserViewCatalog");
        assertEquals("java", browserViewCatalog.get("defaultFamily"));
        assertTrue(((List<?>) browserViewCatalog.get("availableFamilies")).contains("java"),
            () -> "Expected Java browser-view family. browserViewCatalog=" + browserViewCatalog);
    }


    private static String viewpointAvailability(ArchitectureIndexDocument document, String viewpointId) {
        return document.viewpoints().stream()
            .filter(viewpoint -> viewpointId.equals(viewpoint.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing viewpoint=" + viewpointId))
            .availability();
    }

    private static List<String> preferredDependencyViews(ArchitectureIndexDocument document, String viewpointId) {
        return document.viewpoints().stream()
            .filter(viewpoint -> viewpointId.equals(viewpoint.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing viewpoint=" + viewpointId))
            .preferredDependencyViews();
    }

    private static List<String> evidenceSources(ArchitectureIndexDocument document, String viewpointId) {
        return document.viewpoints().stream()
            .filter(viewpoint -> viewpointId.equals(viewpoint.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing viewpoint=" + viewpointId))
            .evidenceSources();
    }

    private static void assertBrowserView(
        List<Map<String, Object>> views,
        String id,
        String framework,
        String typeDependencyView,
        String moduleDependencyView,
        String frameworkRelationship
    ) {
        Map<String, Object> view = views.stream()
            .filter(candidate -> id.equals(candidate.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing browser view " + id + ". views=" + views));
        assertEquals(framework, view.get("framework"));
        assertEquals(typeDependencyView, view.get("typeDependencyView"));
        assertEquals(moduleDependencyView, view.get("moduleDependencyView"));
        assertEquals(Boolean.TRUE, view.get("available"));
        assertTrue(((Number) view.get("typeDependencyCount")).intValue() > 0 || ((Number) view.get("moduleDependencyCount")).intValue() > 0,
            () -> "Expected browser view " + id + " to expose dependencies. view=" + view);
        assertTrue(((List<?>) view.get("frameworkRelationships")).contains(frameworkRelationship),
            () -> "Expected framework relationship " + frameworkRelationship + " in view=" + view);
    }

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        Path fixtureRoot = Path.of("src/test/resources/fixtures/java/java-backend-baseline");
        FileInventory inventory = new FileInventoryScanner().scan(fixtureRoot, InventoryScanOptions.defaults());
        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry parserRegistry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(parserRegistry).parseInventory(fixtureRoot, inventory);

        assertTrue(parseBatch.results().stream().allMatch(result -> result.status() == ParseStatus.SUCCESS),
            () -> "Expected successful parse for Java browser-view fixture. Results=" + summarizeFailures(parseBatch));
        assertTrue(parseBatch.results().stream().allMatch(JavaFrameworkBrowserViewsRegressionTest::usesRealTreeSitterBackend),
            () -> "Expected real Tree-sitter backend for Java browser-view fixture. Results=" + parseBatch.results());

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
