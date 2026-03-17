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

class ArchitectureIrDependencyViewAssemblySupportTest {
    @Test
    void assemblesAndFinalizesDependencyViewsThroughDedicatedSupport() {
        ArchitectureEntity sourceType = new ArchitectureEntity(
            "entity:type:source",
            EntityKind.CLASS,
            "com.example.api.OrderService",
            EntityOrigin.OBSERVED,
            List.of(),
            Map.of("qualifiedName", "com.example.api.OrderService", "packageName", "com.example.api", "sourceRoot", "src/main/java")
        );
        ArchitectureEntity targetType = new ArchitectureEntity(
            "entity:type:target",
            EntityKind.CLASS,
            "com.example.domain.Order",
            EntityOrigin.OBSERVED,
            List.of(),
            Map.of("qualifiedName", "com.example.domain.Order", "packageName", "com.example.domain", "sourceRoot", "src/main/java")
        );
        ArchitectureEntity sourceRoot = new ArchitectureEntity(
            "entity:module:srcmainjava",
            EntityKind.MODULE,
            "src/main/java",
            EntityOrigin.OBSERVED,
            List.of(),
            Map.of("logicalRole", "source-root")
        );

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(sourceType.id(), sourceType);
        entitiesById.put(targetType.id(), targetType);
        entitiesById.put(sourceRoot.id(), sourceRoot);

        ArchitectureRelationship typeDependency = new ArchitectureRelationship(
            "rel:field",
            RelationshipKind.DEPENDS_ON,
            sourceType.id(),
            targetType.id(),
            "OrderService -> Order",
            List.of(),
            Map.of("dependencySource", "field", "dependencyCategory", "composition")
        );

        Map<String, Object> dependencyViews = ArchitectureIrDependencyViewAssemblySupport.buildDependencyViews(
            new ArchitectureIrDependencyViewAssemblyInputs(
                List.of(typeDependency),
                entitiesById,
                ArchitectureIrAssemblyCompositionSupport.observedTypesByQualifiedName(entitiesById)
            )
        );

        List<Map<String, Object>> typeDependencies = (List<Map<String, Object>>) dependencyViews.get("typeDependencies");
        List<Map<String, Object>> packageDependencies = (List<Map<String, Object>>) dependencyViews.get("packageDependencies");
        List<Map<String, Object>> moduleDependencies = (List<Map<String, Object>>) dependencyViews.get("moduleDependencies");
        List<String> primaryViews = (List<String>) dependencyViews.get("primaryArchitectureViews");

        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "entity:type:source".equals(dep.get("sourceTypeId"))
                && "entity:type:target".equals(dep.get("targetTypeId"))
                && Integer.valueOf(1).equals(dep.get("evidenceRelationshipCount"))
        ));
        assertTrue(packageDependencies.stream().anyMatch(dep ->
            "com.example.api".equals(dep.get("sourcePackageName"))
                && "com.example.domain".equals(dep.get("targetPackageName"))
        ));
        assertTrue(moduleDependencies.stream().anyMatch(dep ->
            "src/main/java".equals(dep.get("sourceModuleName"))
                && "src/main/java".equals(dep.get("targetModuleName"))
                && Boolean.TRUE.equals(dep.get("sameModule"))
        ));
        assertTrue(primaryViews.contains("dependency"));
        assertEquals(Boolean.TRUE, dependencyViews.get("hasJavaBrowserView"));
    }
}
