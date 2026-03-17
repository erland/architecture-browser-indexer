package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ArchitectureIrAssemblyCompatibilitySupportTest {

    @Test
    void preservesObservedTypeLookupAndPackageBoundaryCompatibilityWrappers() {
        ArchitectureEntity observedType = new ArchitectureEntity(
            "type:observed:OrderService",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.orders.OrderService", "packageName", "com.example.orders")
        );
        ArchitectureEntity inferredType = new ArchitectureEntity(
            "type:inferred:OrderService",
            EntityKind.CLASS,
            EntityOrigin.INFERRED,
            "com.example.orders.OrderService",
            "com.example.orders.OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.orders.OrderService")
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
            observedType.id(), observedType,
            inferredType.id(), inferredType,
            packageEntity.id(), packageEntity
        );

        Map<String, ArchitectureEntity> observedTypes = ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById);
        ArchitectureEntity canonical = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(inferredType, observedTypes);

        assertSame(observedType, canonical);
        assertEquals("com.example.orders", ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(inferredType));
        assertEquals("internal", ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName("com.example.orders", entitiesById));
        assertEquals("observed-source-package", ArchitectureIrAssemblyCompatibilitySupport.packageClassificationForName("com.example.orders", entitiesById));
        assertEquals("scope:repo", ArchitectureIrAssemblyCompatibilitySupport.normalizeScopeId(null, "scope:repo"));
    }
}
