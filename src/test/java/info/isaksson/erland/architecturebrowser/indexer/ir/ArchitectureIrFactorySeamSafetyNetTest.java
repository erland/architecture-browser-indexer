package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactorySeamSafetyNetTest {

    @Test
    void preservesNestedEntityAndRelationshipMetadataWhenMappingIntoFinalIr() {
        FileInventory inventory = new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of());
        SourceReference sourceReference = new SourceReference(
            "src/main/java/com/example/orders/OrderService.java",
            10,
            20,
            "class OrderService {}",
            Map.of("snippetKind", "declaration")
        );

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact(
                    "entity:class:orderservice",
                    EntityKind.CLASS,
                    EntityOrigin.OBSERVED,
                    "OrderService",
                    "OrderService",
                    "scope:file:orderservice",
                    List.of(sourceReference),
                    Map.of(
                        "qualifiedName", "com.example.orders.OrderService",
                        "declarationKind", "class",
                        "frameworkHints", List.of("jax-rs", "cdi"),
                        "nested", Map.of("stable", true, "version", 1)
                    )
                ),
                new ExtractedEntityFact(
                    "entity:class:requestcontext",
                    EntityKind.CLASS,
                    EntityOrigin.INFERRED,
                    "com.example.shared.RequestContext",
                    "com.example.shared.RequestContext",
                    "scope:file:orderservice",
                    List.of(),
                    Map.of("qualifiedName", "com.example.shared.RequestContext")
                )
            ),
            List.of(
                new ExtractedRelationshipFact(
                    "rel:orderservice:requestcontext",
                    RelationshipKind.DEPENDS_ON,
                    "entity:class:orderservice",
                    "entity:class:requestcontext",
                    "com.example.shared.RequestContext",
                    List.of(sourceReference),
                    Map.of(
                        "dependencySource", "parameterType",
                        "dependencyCategory", "api",
                        "architectureViewKinds", List.of("endpoint", "framework"),
                        "evidence", Map.of("stable", true)
                    )
                )
            ),
            List.of(),
            new ExtractionSummary(1, 1, Map.of("java", 1), Map.of("structural", 1), 2, 1)
        );

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-16T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            null,
            extraction
        );

        ArchitectureEntity entity = document.entities().stream()
            .filter(item -> "entity:class:orderservice".equals(item.id()))
            .findFirst()
            .orElseThrow();
        ArchitectureRelationship relationship = document.relationships().stream()
            .filter(item -> "rel:orderservice:requestcontext".equals(item.id()))
            .findFirst()
            .orElseThrow();

        assertEquals("com.example.orders.OrderService", entity.metadata().get("qualifiedName"));
        assertEquals(List.of("jax-rs", "cdi"), entity.metadata().get("frameworkHints"));
        assertEquals(Map.of("stable", true, "version", 1), entity.metadata().get("nested"));
        assertEquals("parameterType", relationship.metadata().get("dependencySource"));
        assertEquals("api", relationship.metadata().get("dependencyCategory"));
        assertEquals(List.of("endpoint", "framework"), relationship.metadata().get("architectureViewKinds"));
        assertEquals(Map.of("stable", true), relationship.metadata().get("evidence"));
    }

    @Test
    void buildsPackageMetricsBoundarySummaryAndBrowserViewCatalogForJavaBackendFixture() {
        ArchitectureIndexDocument document = ArchitectureIrFactoryJavaBackendSafetyNetTestData.buildDocumentFromFixture();

        @SuppressWarnings("unchecked")
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        assertNotNull(dependencyViews);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> packageMetrics = (List<Map<String, Object>>) dependencyViews.get("packageMetrics");
        assertFalse(packageMetrics.isEmpty());
        assertTrue(packageMetrics.stream().anyMatch(metric -> metric.containsKey("packageName") && metric.containsKey("outgoingDependencyCount")));

        @SuppressWarnings("unchecked")
        Map<String, Object> boundarySummary = (Map<String, Object>) dependencyViews.get("boundarySummary");
        assertNotNull(boundarySummary);
        assertTrue(boundarySummary.containsKey("typeInternalCount"));
        assertTrue(boundarySummary.containsKey("typeExternalCount"));
        assertTrue(boundarySummary.containsKey("packageInternalCount"));
        assertTrue(boundarySummary.containsKey("packageExternalCount"));
        assertTrue(boundarySummary.containsKey("moduleInternalCount"));
        assertTrue(boundarySummary.containsKey("moduleExternalCount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> browserViewCatalog = (Map<String, Object>) dependencyViews.get("browserViewCatalog");
        assertNotNull(browserViewCatalog);
        assertTrue(browserViewCatalog.containsKey("families"));
        assertTrue(browserViewCatalog.containsKey("availableFamilies"));
        assertTrue(String.valueOf(browserViewCatalog).contains("java"));
    }
}
