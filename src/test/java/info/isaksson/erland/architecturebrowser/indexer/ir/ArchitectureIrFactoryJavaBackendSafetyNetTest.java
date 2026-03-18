package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
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

class ArchitectureIrFactoryJavaBackendSafetyNetTest {

    @Test
    void javaBackendFixtureProducesStableIrShapeForRefactoringBaseline() {
        ArchitectureIndexDocument document = buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        assertTrue(document.entities().size() >= 20,
            () -> "Expected a non-trivial entity baseline. Entity count=" + document.entities().size());
        assertTrue(document.relationships().size() >= 20,
            () -> "Expected a non-trivial relationship baseline. Relationship count=" + document.relationships().size());
        assertTrue(document.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.REPOSITORY));
        assertTrue(document.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.DIRECTORY));
        assertTrue(document.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.PACKAGE));
        assertTrue(document.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.FILE));

        assertEquals("SUCCESS", document.runMetadata().outcome().name());
        assertTrue(document.relationships().stream().anyMatch(rel -> rel.architecturalSemantics() != null && rel.architecturalSemantics().contains("serves-request")));
        assertTrue(document.relationships().stream().anyMatch(rel -> rel.architecturalSemantics() != null && rel.architecturalSemantics().contains("invokes-use-case")));
        assertTrue(document.relationships().stream().anyMatch(rel -> rel.architecturalSemantics() != null && rel.architecturalSemantics().contains("accesses-persistence")));
        assertTrue(document.metadata().containsKey("dependencyViews"));
        assertTrue(document.metadata().containsKey("topologySummary"));
        assertTrue(document.metadata().containsKey("extractionSummary"));

        @SuppressWarnings("unchecked")
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        assertTrue(dependencyViews.containsKey("endpointTypeDependencies"));
        assertTrue(dependencyViews.containsKey("entityModelTypeDependencies"));
        assertTrue(dependencyViews.containsKey("observerTypeDependencies"));
        assertTrue(dependencyViews.containsKey("writePathTypeDependencies"));
        assertTrue(dependencyViews.containsKey("javaBrowserViews"));
        assertTrue(dependencyViews.containsKey("browserViewCatalog"));

        @SuppressWarnings("unchecked")
        Map<String, Object> javaBrowserViews = (Map<String, Object>) dependencyViews.get("javaBrowserViews");
        assertEquals("javaEndpointGraph", javaBrowserViews.get("defaultViewId"));
        assertTrue(((List<?>) javaBrowserViews.get("availableViews")).containsAll(List.of(
            "javaEndpointGraph",
            "javaEntityModelGraph",
            "javaEventFlowGraph",
            "javaWritePathGraph"
        )));
    }

    private static ArchitectureIndexDocument buildDocumentFromFixture() {
        return ArchitectureIrFactoryJavaBackendSafetyNetTestData.buildDocumentFromFixture();
    }
}
