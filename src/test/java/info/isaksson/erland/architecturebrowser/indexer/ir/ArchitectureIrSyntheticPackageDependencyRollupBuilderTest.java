package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArchitectureIrSyntheticPackageDependencyRollupBuilderTest {

    @Test
    void synthesizesSinglePackageRollupForCrossPackageTypeDependency() {
        ArchitectureEntity sourceType = new ArchitectureEntity(
            "type:ApiController",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "ApiController",
            "ApiController",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.api.ApiController", "packageName", "com.example.api")
        );
        ArchitectureEntity targetType = new ArchitectureEntity(
            "type:OrderService",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.domain.OrderService", "packageName", "com.example.domain")
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

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(sourceType.id(), sourceType);
        entitiesById.put(targetType.id(), targetType);
        entitiesById.put(sourcePackage.id(), sourcePackage);
        entitiesById.put(targetPackage.id(), targetPackage);

        ArchitectureRelationship typeDependency = new ArchitectureRelationship(
            "rel:type",
            RelationshipKind.DEPENDS_ON,
            sourceType.id(),
            targetType.id(),
            "com.example.domain.OrderService",
            List.of(),
            Map.of("dependencySource", "field", "dependencyCategory", "composition")
        );

        List<ArchitectureRelationship> relationships = ArchitectureIrSyntheticPackageDependencyRollupBuilder.ensurePackageDependencyRelationships(
            List.of(typeDependency),
            entitiesById,
            Map.of()
        );

        String expectedSyntheticId = IdUtils.relationshipId("ir-package-uses", sourcePackage.id(), targetPackage.id(), "");
        ArchitectureRelationship synthetic = relationships.stream()
            .filter(relationship -> relationship.id().equals(expectedSyntheticId))
            .findFirst()
            .orElseThrow();

        assertEquals(2, relationships.size());
        assertEquals(RelationshipKind.USES, synthetic.kind());
        assertEquals("package-package", synthetic.metadata().get("rollup"));
        assertEquals("package", synthetic.metadata().get("dependencyView"));
        assertEquals("com.example.api", synthetic.metadata().get("dependencySourcePackageName"));
        assertEquals("com.example.domain", synthetic.metadata().get("dependencyTargetPackageName"));
        assertEquals("composition", synthetic.metadata().get("dependencyCategory"));
        assertNotNull(synthetic.metadata().get("dependencyTargetPackageClassification"));
    }
}
