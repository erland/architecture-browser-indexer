package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrSeamHardeningTest {

    @Test
    @SuppressWarnings("unchecked")
    void browserViewBuildersPreserveStableIdsAndFamilyCatalog() {
        Map<String, Object> routeDependency = Map.of(
            "frameworkRelationships", List.of("targets"),
            "frameworks", List.of("frontend"),
            "sourceId", "route:/orders",
            "targetId", "component:OrdersPage"
        );
        Map<String, Object> compositionDependency = Map.of(
            "frameworkRelationships", List.of("renders"),
            "frameworks", List.of("react"),
            "sourceId", "component:App",
            "targetId", "component:OrdersPage"
        );
        Map<String, Object> endpointDependency = Map.of(
            "frameworkRelationships", List.of("endpoint"),
            "frameworks", List.of("jax-rs"),
            "architectureViewKinds", List.of("endpoint"),
            "relationshipKind", "EXPOSES",
            "sourceId", "resource:OrderResource",
            "targetId", "endpoint:getOrder"
        );

        Map<String, Object> frontendViews = ArchitectureIrBrowserViewMetadataBuilder.buildFrontendBrowserViews(
            List.of(compositionDependency),
            List.of(),
            List.of(routeDependency),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Map<String, Object> javaViews = ArchitectureIrBrowserViewMetadataBuilder.buildJavaBrowserViews(
            List.of(endpointDependency),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Map<String, Object> catalog = ArchitectureIrBrowserViewMetadataBuilder.buildBrowserViewCatalog(frontendViews, javaViews);

        List<Map<String, Object>> frontendDescriptors = (List<Map<String, Object>>) frontendViews.get("views");
        assertTrue(frontendDescriptors.stream().anyMatch(view -> "routeGraph".equals(view.get("id")) && Boolean.TRUE.equals(view.get("available"))));
        assertTrue(frontendDescriptors.stream().anyMatch(view -> "reactComponentCompositionGraph".equals(view.get("id")) && Boolean.TRUE.equals(view.get("available"))));
        assertEquals(List.of("routeGraph", "reactComponentCompositionGraph"), frontendViews.get("availableViews"));
        assertEquals("routeGraph", frontendViews.get("defaultViewId"));

        List<Map<String, Object>> families = (List<Map<String, Object>>) catalog.get("families");
        assertTrue(families.stream().anyMatch(family -> "frontend".equals(family.get("id"))));
        assertTrue(families.stream().anyMatch(family -> "java".equals(family.get("id"))));
        assertEquals(List.of("frontend", "java"), catalog.get("availableFamilies"));
        assertEquals("frontend", catalog.get("defaultFamily"));
    }

    @Test
    void packageMetricsBoundarySummaryAndDependencyMetadataStayStable() {
        ArchitectureEntity packageEntity = new ArchitectureEntity(
            "pkg:orders",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "com.example.orders",
            "com.example.orders",
            "repo",
            List.of(new SourceReference("src/main/java/com/example/orders/package-info.java", 1, 1, "package com.example.orders;", Map.of())),
            Map.of("logicalRole", "package", "language", "java")
        );
        ArchitectureEntity classEntity = new ArchitectureEntity(
            "type:OrderService",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "repo",
            List.of(),
            Map.of("qualifiedName", "com.example.orders.OrderService", "declarationKind", "class")
        );
        ArchitectureEntity targetEntity = new ArchitectureEntity(
            "type:OrderRepository",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderRepository",
            "OrderRepository",
            "repo",
            List.of(),
            Map.of()
        );
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:import",
            RelationshipKind.DEPENDS_ON,
            classEntity.id(),
            targetEntity.id(),
            "imports",
            List.of(),
            Map.of("dependencySource", "typescript:import")
        );

        List<Map<String, Object>> packageMetrics = ArchitectureIrPackageMetricsBoundaryBuilder.buildPackageMetrics(
            Map.of(packageEntity.id(), packageEntity, classEntity.id(), classEntity),
            List.of(Map.of("sourcePackageName", "com.example.orders", "targetPackageName", "com.example.orders", "targetBoundary", "internal"))
        );
        Map<String, Object> boundarySummary = ArchitectureIrPackageMetricsBoundaryBuilder.buildBoundarySummary(
            List.of(Map.of("targetBoundary", "internal"), Map.of("targetBoundary", "external")),
            List.of(Map.of("targetBoundary", "internal")),
            List.of(Map.of("targetBoundary", "external"))
        );
        Map<String, Object> importEvidenceMetadata = ArchitectureIrDependencyMetadataSupport.shapeImportEvidenceMetadata(
            relationship,
            classEntity,
            targetEntity,
            new LinkedHashMap<>(relationship.metadata()),
            true,
            false,
            "internal",
            "service"
        );
        LinkedHashMap<String, Object> summaryMetadata = new LinkedHashMap<>();
        ArchitectureIrDependencyMetadataSupport.putSummaryCollections(
            summaryMetadata,
            Set.of("java:type"),
            Set.of("inheritance"),
            Set.of("jax-rs"),
            Set.of("endpoint"),
            Set.of("endpoint"),
            Set.of("rel:import"),
            Set.of("imports")
        );

        assertEquals(1, packageMetrics.size());
        assertEquals("com.example.orders", packageMetrics.getFirst().get("packageName"));
        assertEquals(1, packageMetrics.getFirst().get("declaredTypeCount"));
        assertEquals(1, packageMetrics.getFirst().get("incomingDependencyCount"));
        assertEquals(1, packageMetrics.getFirst().get("outgoingDependencyCount"));

        assertEquals(1, boundarySummary.get("typeInternalCount"));
        assertEquals(1, boundarySummary.get("typeExternalCount"));
        assertEquals(1, boundarySummary.get("packageInternalCount"));
        assertEquals(1, boundarySummary.get("moduleExternalCount"));

        assertEquals("evidence", importEvidenceMetadata.get("dependencyView"));
        assertEquals("supporting-evidence", importEvidenceMetadata.get("dependencyTier"));
        assertEquals(List.of("java:type"), summaryMetadata.get("dependencySources"));
        assertEquals(List.of("endpoint"), summaryMetadata.get("frameworkRelationships"));
        assertEquals(List.of("endpoint"), summaryMetadata.get("architectureViewKinds"));
    }
}
