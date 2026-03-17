package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrMetadataShapingSupportsTest {

    @Test
    @SuppressWarnings("unchecked")
    void browserMetadataSupportsExposeStableFamiliesAndAvailableViews() {
        Map<String, Object> routeDependency = Map.of(
            "frameworkRelationships", List.of("targets"),
            "frameworks", List.of("frontend"),
            "sourceId", "route:/orders",
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

        Map<String, Object> frontend = ArchitectureIrFrontendBrowserViewSupport.buildFrontendBrowserViews(
            List.of(), List.of(), List.of(routeDependency), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        Map<String, Object> java = ArchitectureIrJavaBrowserViewSupport.buildJavaBrowserViews(
            List.of(endpointDependency), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        Map<String, Object> catalog = ArchitectureIrBrowserViewFamilyCatalogSupport.buildBrowserViewCatalog(frontend, java);

        assertEquals(List.of("routeGraph"), frontend.get("availableViews"));
        assertEquals(List.of("javaEndpointGraph"), java.get("availableViews"));
        assertEquals(List.of("frontend", "java"), catalog.get("availableFamilies"));
        List<Map<String, Object>> families = (List<Map<String, Object>>) catalog.get("families");
        assertTrue(families.stream().anyMatch(family -> "frontend".equals(family.get("id"))));
        assertTrue(families.stream().anyMatch(family -> "java".equals(family.get("id"))));
    }

    @Test
    void dependencyCategoryEnrichersSplitTypePackageAndModuleMetadata() {
        ArchitectureEntity sourceType = new ArchitectureEntity(
            "type:OrderResource", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderResource", "OrderResource", "src",
            List.of(), Map.of("qualifiedName", "com.example.api.OrderResource")
        );
        ArchitectureEntity targetType = new ArchitectureEntity(
            "type:OrderService", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "OrderService", "src",
            List.of(), Map.of("qualifiedName", "com.example.service.OrderService")
        );
        ArchitectureRelationship typeRelationship = new ArchitectureRelationship(
            "rel:type", RelationshipKind.DEPENDS_ON, sourceType.id(), targetType.id(), "depends on", List.of(), Map.of()
        );

        Map<String, Object> typeMetadata = ArchitectureIrTypeDependencyCategoryEnricher.enrich(
            typeRelationship, sourceType, targetType, new LinkedHashMap<>(), Map.of()
        );
        assertEquals("type", typeMetadata.get("dependencyView"));
        assertEquals("com.example.api", typeMetadata.get("dependencySourcePackageName"));
        assertEquals("com.example.service", typeMetadata.get("dependencyTargetPackageName"));

        ArchitectureEntity sourcePackage = new ArchitectureEntity(
            "pkg:api", EntityKind.MODULE, EntityOrigin.OBSERVED, "com.example.api", "com.example.api", "src",
            List.of(), Map.of("logicalRole", "package")
        );
        ArchitectureEntity targetPackage = new ArchitectureEntity(
            "pkg:service", EntityKind.MODULE, EntityOrigin.OBSERVED, "com.example.service", "com.example.service", "src",
            List.of(), Map.of("logicalRole", "package")
        );
        ArchitectureRelationship packageRelationship = new ArchitectureRelationship(
            "rel:pkg", RelationshipKind.USES, sourcePackage.id(), targetPackage.id(), "uses", List.of(), Map.of("rollup", "package-package")
        );
        Map<String, Object> packageMetadata = ArchitectureIrPackageDependencyCategoryEnricher.enrich(
            packageRelationship, sourcePackage, targetPackage, new LinkedHashMap<>(), Map.of(sourcePackage.id(), sourcePackage, targetPackage.id(), targetPackage)
        );
        assertEquals("package", packageMetadata.get("dependencyView"));
        assertEquals("com.example.service", packageMetadata.get("dependencyTargetPackageName"));

        ArchitectureEntity sourceModule = new ArchitectureEntity(
            "mod:main", EntityKind.MODULE, EntityOrigin.OBSERVED, "src", "src", "src",
            List.of(), Map.of("logicalRole", "source-root")
        );
        ArchitectureEntity targetModule = new ArchitectureEntity(
            "mod:test", EntityKind.MODULE, EntityOrigin.OBSERVED, "test", "test", "test",
            List.of(), Map.of("logicalRole", "source-root")
        );
        ArchitectureRelationship moduleRelationship = new ArchitectureRelationship(
            "rel:mod", RelationshipKind.USES, sourceModule.id(), targetModule.id(), "uses", List.of(), Map.of("rollup", "module-module")
        );
        Map<String, Object> moduleMetadata = ArchitectureIrModuleDependencyCategoryEnricher.enrich(
            moduleRelationship, sourceModule, targetModule, new LinkedHashMap<>(), Map.of(sourceModule.id(), sourceModule, targetModule.id(), targetModule)
        );
        assertEquals("module", moduleMetadata.get("dependencyView"));
        assertEquals("test", moduleMetadata.get("dependencyTargetModuleName"));
    }
}
