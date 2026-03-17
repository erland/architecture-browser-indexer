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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrDependencyAssemblyPhasesTest {
    @Test
    void normalizesAndBuildsDependencyViewsThroughExplicitPhases() {
        ArchitectureEntity sourceType = new ArchitectureEntity(
            "entity:type:source",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.api.OrderService",
            "com.example.api.OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.api.OrderService", "packageName", "com.example.api", "sourceRoot", "src/main/java")
        );
        ArchitectureEntity targetType = new ArchitectureEntity(
            "entity:type:target",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.domain.Order",
            "com.example.domain.Order",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.domain.Order", "packageName", "com.example.domain", "sourceRoot", "src/main/java")
        );
        ArchitectureEntity sourceRoot = new ArchitectureEntity(
            "entity:module:srcmainjava",
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

        ArchitectureIrDependencyViewAssemblyInputs inputs = new ArchitectureIrDependencyViewAssemblyInputs(
            List.of(typeDependency),
            entitiesById,
            ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById)
        );

        List<ArchitectureIrNormalizedDependencyContext> contexts = ArchitectureIrDependencyNormalizationSupport.normalize(inputs);
        List<Map<String, Object>> typeDependencies = ArchitectureIrTypeDependencyViewBuilder.build(contexts);
        List<Map<String, Object>> packageDependencies = ArchitectureIrPackageDependencyViewBuilder.build(contexts);
        List<Map<String, Object>> moduleDependencies = ArchitectureIrModuleDependencyViewBuilder.build(contexts);
        List<Map<String, Object>> evidenceDependencies = ArchitectureIrEvidenceDependencyViewBuilder.build(contexts);

        assertEquals(1, contexts.size());
        assertTrue(contexts.get(0).typeDependencyRelationship());
        assertFalse(contexts.get(0).importEvidenceRelationship());
        assertEquals(1, typeDependencies.size());
        assertEquals(1, packageDependencies.size());
        assertEquals(1, moduleDependencies.size());
        assertTrue(evidenceDependencies.isEmpty());
    }
}
