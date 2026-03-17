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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrAssemblyCompositionSupportSafetyNetTest {

    @Test
    void preservesObservedTypeCanonicalizationAndDependencyMetadataEnrichment() {
        ArchitectureEntity observedOrderService = new ArchitectureEntity(
            "type:observed:OrderService",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(new SourceReference("src/main/java/com/example/orders/OrderService.java", 1, 1, "class OrderService {}", Map.of())),
            Map.of("qualifiedName", "com.example.orders.OrderService", "packageName", "com.example.orders", "relativePath", "src/main/java/com/example/orders/OrderService.java")
        );
        ArchitectureEntity inferredOrderService = new ArchitectureEntity(
            "type:inferred:OrderService",
            EntityKind.CLASS,
            EntityOrigin.INFERRED,
            "com.example.orders.OrderService",
            "com.example.orders.OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.orders.OrderService")
        );
        ArchitectureEntity externalRequestContext = new ArchitectureEntity(
            "type:external:RequestContext",
            EntityKind.CLASS,
            EntityOrigin.INFERRED,
            "org.springframework.web.context.request.RequestContext",
            "RequestContext",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "org.springframework.web.context.request.RequestContext")
        );
        ArchitectureEntity fileModule = new ArchitectureEntity(
            "module:file:orderservice",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "src/main/java/com/example/orders/OrderService.java",
            "OrderService.java",
            "scope:repo",
            List.of(new SourceReference("src/main/java/com/example/orders/OrderService.java", 1, 1, "import org.springframework.web.context.request.RequestContext;", Map.of())),
            Map.of("logicalRole", "file", "relativePath", "src/main/java/com/example/orders/OrderService.java")
        );
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

        Map<String, ArchitectureEntity> entitiesById = Map.of(
            observedOrderService.id(), observedOrderService,
            inferredOrderService.id(), inferredOrderService,
            externalRequestContext.id(), externalRequestContext,
            fileModule.id(), fileModule,
            packageEntity.id(), packageEntity
        );

        ArchitectureRelationship typeDependency = new ArchitectureRelationship(
            "rel:type-dep",
            RelationshipKind.DEPENDS_ON,
            inferredOrderService.id(),
            externalRequestContext.id(),
            "org.springframework.web.context.request.RequestContext",
            List.of(),
            Map.of("dependencySource", "parameterType", "dependencyCategory", "api")
        );
        ArchitectureRelationship importEvidence = new ArchitectureRelationship(
            "rel:import-evidence",
            RelationshipKind.DEPENDS_ON,
            fileModule.id(),
            externalRequestContext.id(),
            "org.springframework.web.context.request.RequestContext",
            List.of(),
            Map.of("dependencySource", "import")
        );

        Map<String, ArchitectureEntity> observedTypes = ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById);
        List<ArchitectureRelationship> enriched = ArchitectureIrAssemblyCompositionSupport.enrichDependencyRelationshipMetadata(
            List.of(typeDependency, importEvidence),
            entitiesById,
            observedTypes
        );

        ArchitectureRelationship canonicalizedTypeDependency = enriched.stream()
            .filter(relationship -> relationship.id().equals("rel:type-dep"))
            .findFirst()
            .orElseThrow();
        ArchitectureRelationship evidenceRelationship = enriched.stream()
            .filter(relationship -> relationship.id().equals("rel:import-evidence"))
            .findFirst()
            .orElseThrow();

        assertEquals(inferredOrderService.id(), canonicalizedTypeDependency.fromEntityId());
        assertEquals(observedOrderService.id(), canonicalizedTypeDependency.metadata().get("dependencySourceTypeId"));
        assertEquals("type", canonicalizedTypeDependency.metadata().get("dependencyView"));
        assertEquals("external", canonicalizedTypeDependency.metadata().get("dependencyTargetBoundary"));
        assertEquals("external-or-inferred-type", canonicalizedTypeDependency.metadata().get("dependencyTargetClassification"));

        assertNotNull(evidenceRelationship.metadata());
        assertEquals("import", evidenceRelationship.metadata().get("dependencySource"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesSyntheticPackageRollupsDependencyViewsAndScopeNormalization() {
        ArchitectureEntity sourceType = new ArchitectureEntity(
            "type:ApiController",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "ApiController",
            "ApiController",
            "scope:repo",
            List.of(new SourceReference("src/main/java/com/example/api/ApiController.java", 1, 1, "class ApiController {}", Map.of())),
            Map.of("qualifiedName", "com.example.api.ApiController", "packageName", "com.example.api", "relativePath", "src/main/java/com/example/api/ApiController.java")
        );
        ArchitectureEntity targetType = new ArchitectureEntity(
            "type:OrderService",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(new SourceReference("src/main/java/com/example/domain/OrderService.java", 1, 1, "class OrderService {}", Map.of())),
            Map.of("qualifiedName", "com.example.domain.OrderService", "packageName", "com.example.domain", "relativePath", "src/main/java/com/example/domain/OrderService.java")
        );
        ArchitectureEntity sourcePackage = new ArchitectureEntity(
            "pkg:api",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "com.example.api",
            "com.example.api",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "package", "language", "java")
        );
        ArchitectureEntity targetPackage = new ArchitectureEntity(
            "pkg:domain",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "com.example.domain",
            "com.example.domain",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "package", "language", "java")
        );
        ArchitectureEntity sourceRoot = new ArchitectureEntity(
            "source-root:main-java",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "src/main/java",
            "src/main/java",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "source-root")
        );

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(sourceType.id(), sourceType);
        entitiesById.put(targetType.id(), targetType);
        entitiesById.put(sourcePackage.id(), sourcePackage);
        entitiesById.put(targetPackage.id(), targetPackage);
        entitiesById.put(sourceRoot.id(), sourceRoot);

        ArchitectureRelationship typeDependency = new ArchitectureRelationship(
            "rel:field",
            RelationshipKind.DEPENDS_ON,
            sourceType.id(),
            targetType.id(),
            "com.example.domain.OrderService",
            List.of(),
            Map.of("dependencySource", "field", "dependencyCategory", "composition")
        );

        List<ArchitectureRelationship> relationships = ArchitectureIrAssemblyCompositionSupport.ensurePackageDependencyRelationships(
            List.of(typeDependency),
            entitiesById,
            ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById)
        );
        Map<String, Object> dependencyViews = ArchitectureIrAssemblyCompositionSupport.buildDependencyViews(
            ArchitectureIrAssemblyCompositionSupport.enrichDependencyRelationshipMetadata(
                relationships,
                entitiesById,
                ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById)
            ),
            entitiesById,
            ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById)
        );
        Map<String, ArchitectureEntity> enrichedEntities = ArchitectureIrAssemblyCompositionSupport.enrichPackageEntities(entitiesById, dependencyViews);

        List<Map<String, Object>> packageDependencies = (List<Map<String, Object>>) dependencyViews.get("packageDependencies");
        List<Map<String, Object>> moduleDependencies = (List<Map<String, Object>>) dependencyViews.get("moduleDependencies");
        List<Map<String, Object>> packageMetrics = (List<Map<String, Object>>) dependencyViews.get("packageMetrics");
        Map<String, Object> boundarySummary = (Map<String, Object>) dependencyViews.get("boundarySummary");
        ArchitectureEntity enrichedPackage = enrichedEntities.get(sourcePackage.id());

        assertTrue(relationships.stream().anyMatch(relationship -> relationship.kind() == RelationshipKind.USES
            && "package".equals(relationship.metadata().get("dependencyView"))
            && "com.example.api".equals(relationship.metadata().get("dependencySourcePackageName"))
            && "com.example.domain".equals(relationship.metadata().get("dependencyTargetPackageName"))));
        assertTrue(packageDependencies.stream().anyMatch(dep ->
            "com.example.api".equals(dep.get("sourcePackageName"))
                && "com.example.domain".equals(dep.get("targetPackageName"))
                && Boolean.TRUE.equals(dep.get("internalTarget"))
                && ((List<?>) dep.get("dependencySources")).contains("field")
        ));
        assertTrue(moduleDependencies.stream().anyMatch(dep ->
            "src/main/java".equals(dep.get("sourceModuleName"))
                && "src/main/java".equals(dep.get("targetModuleName"))
                && Boolean.TRUE.equals(dep.get("sameModule"))
        ));
        assertTrue(packageMetrics.stream().anyMatch(metric ->
            "com.example.api".equals(metric.get("packageName"))
                && Integer.valueOf(1).equals(metric.get("outgoingDependencyCount"))
        ));
        assertEquals(1, boundarySummary.get("typeInternalCount"));
        assertEquals(0, boundarySummary.get("typeExternalCount"));
        assertEquals(1, enrichedPackage.metadata().get("outgoingDependencyCount"));
        assertEquals("scope:repo", ArchitectureIrAssemblyCompatibilitySupport.normalizeScopeId(null, "scope:repo"));
        assertEquals("scope:file:demo", ArchitectureIrAssemblyCompatibilitySupport.normalizeScopeId("scope:file:demo", "scope:repo"));
    }
}
