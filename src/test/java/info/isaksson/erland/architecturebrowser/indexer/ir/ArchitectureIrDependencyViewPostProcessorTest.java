package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrDependencyViewPostProcessorTest {

    @Test
    @SuppressWarnings("unchecked")
    void finalizesFrameworkAwareViewsRecommendedEntryPointsAndPackageMetrics() {
        ArchitectureEntity packageEntity = new ArchitectureEntity(
            "pkg:orders",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "com.example.orders",
            "com.example.orders",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "package", "language", "java")
        );
        ArchitectureEntity orderService = new ArchitectureEntity(
            "type:OrderService",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.orders.OrderService", "packageName", "com.example.orders")
        );

        Map<String, Object> endpointTypeDependency = Map.of(
            "sourceId", "resource:OrderResource",
            "targetId", "endpoint:getOrder",
            "targetBoundary", "internal",
            "architectureViewKinds", List.of("endpoint"),
            "frameworkRelationships", List.of("endpoint")
        );
        Map<String, Object> packageDependency = Map.of(
            "sourcePackageName", "com.example.orders",
            "targetPackageName", "com.example.orders",
            "targetBoundary", "internal"
        );
        Map<String, Object> evidenceDependency = Map.of(
            "sourceId", "module:file:orderservice",
            "targetId", "module:file:requestcontext",
            "dependencyTier", "supporting-evidence"
        );

        Map<String, Object> dependencyViews = ArchitectureIrDependencyViewPostProcessor.finalizeDependencyViews(
            Map.of(packageEntity.id(), packageEntity, orderService.id(), orderService),
            List.of(),
            List.of(endpointTypeDependency),
            List.of(packageDependency),
            List.of(),
            List.of(evidenceDependency)
        );

        List<String> recommendedEntryPoints = (List<String>) dependencyViews.get("recommendedEntryPoints");
        List<String> primaryArchitectureViews = (List<String>) dependencyViews.get("primaryArchitectureViews");
        List<Map<String, Object>> packageMetrics = (List<Map<String, Object>>) dependencyViews.get("packageMetrics");
        Map<String, Object> javaBrowserViews = (Map<String, Object>) dependencyViews.get("javaBrowserViews");
        Map<String, Object> evidenceStatus = (Map<String, Object>) dependencyViews.get("evidenceStatus");

        assertTrue(recommendedEntryPoints.containsAll(List.of(
            "packageDependencies",
            "typeDependencies",
            "moduleDependencies",
            "endpointTypeDependencies",
            "evidenceDependencies"
        )));
        assertTrue(primaryArchitectureViews.containsAll(List.of(
            "packageDependencies",
            "typeDependencies",
            "moduleDependencies",
            "endpointTypeDependencies"
        )));
        assertEquals(1, packageMetrics.size());
        assertEquals("com.example.orders", packageMetrics.getFirst().get("packageName"));
        assertTrue(((List<String>) javaBrowserViews.get("availableViews")).contains("javaEndpointGraph"));
        assertEquals("supporting-evidence", evidenceStatus.get("fileImportDependencies"));
        assertEquals(Boolean.FALSE, evidenceStatus.get("recommendedForArchitectureViews"));
    }
}
