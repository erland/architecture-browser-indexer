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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrAssemblyCompositionOrchestrationTest {

    @Test
    @SuppressWarnings("unchecked")
    void composeCoordinatesDependencyMetadataRollupsViewsAndPackageEnrichment() {
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

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(sourceType.id(), sourceType);
        entitiesById.put(targetType.id(), targetType);
        entitiesById.put(sourcePackage.id(), sourcePackage);
        entitiesById.put(targetPackage.id(), targetPackage);

        ArchitectureIrAssemblyCompositionResult composition = ArchitectureIrAssemblyCompositionSupport.compose(
            new ArchitectureIrAssemblyCompositionInputs(
                List.of(new ArchitectureRelationship(
                    "rel:field",
                    RelationshipKind.DEPENDS_ON,
                    sourceType.id(),
                    targetType.id(),
                    "com.example.domain.OrderService",
                    List.of(),
                    Map.of("dependencySource", "field", "dependencyCategory", "composition")
                )),
                entitiesById,
                ArchitectureIrAssemblyCompatibilitySupport.observedTypesByQualifiedName(entitiesById)
            )
        );

        List<Map<String, Object>> packageDependencies = (List<Map<String, Object>>) composition.dependencyViews().get("packageDependencies");
        ArchitectureEntity enrichedPackage = composition.enrichedEntitiesById().get(sourcePackage.id());

        assertTrue(composition.relationships().stream().anyMatch(relationship -> relationship.kind() == RelationshipKind.USES
            && "package".equals(relationship.metadata().get("dependencyView"))
            && "com.example.api".equals(relationship.metadata().get("dependencySourcePackageName"))
            && "com.example.domain".equals(relationship.metadata().get("dependencyTargetPackageName"))));
        assertTrue(packageDependencies.stream().anyMatch(dep ->
            "com.example.api".equals(dep.get("sourcePackageName"))
                && "com.example.domain".equals(dep.get("targetPackageName"))
                && Boolean.TRUE.equals(dep.get("internalTarget"))
        ));
        assertEquals(1, enrichedPackage.metadata().get("outgoingDependencyCount"));
    }
}
