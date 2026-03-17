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

class ArchitectureIrDependencyEnrichmentFamiliesTest {

    @Test
    void genericDependencyCategoryEnricherShapesTypeDependencies() {
        ArchitectureEntity source = new ArchitectureEntity(
            "type:source",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.api.OrderResource",
            "OrderResource",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.api.OrderResource", "packageName", "com.example.api")
        );
        ArchitectureEntity target = new ArchitectureEntity(
            "type:target",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.service.OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.service.OrderService", "packageName", "com.example.service")
        );
        ArchitectureEntity sourcePackage = new ArchitectureEntity(
            "pkg:api",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "com.example.api",
            "com.example.api",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "package")
        );
        ArchitectureEntity targetPackage = new ArchitectureEntity(
            "pkg:service",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "com.example.service",
            "com.example.service",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "package")
        );
        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(source.id(), source);
        entitiesById.put(target.id(), target);
        entitiesById.put(sourcePackage.id(), sourcePackage);
        entitiesById.put(targetPackage.id(), targetPackage);

        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:type",
            RelationshipKind.DEPENDS_ON,
            source.id(),
            target.id(),
            "depends",
            List.of(),
            Map.of()
        );

        Map<String, Object> metadata = ArchitectureIrGenericDependencyCategoryEnricher.enrich(
            relationship,
            source,
            target,
            new LinkedHashMap<>(),
            entitiesById
        );

        assertEquals("type", metadata.get("dependencyView"));
        assertEquals("com.example.api", metadata.get("dependencySourcePackageName"));
        assertEquals("com.example.service", metadata.get("dependencyTargetPackageName"));
        assertEquals("internal", metadata.get("dependencyTargetBoundary"));
    }

    @Test
    void evidenceEnricherShapesImportEvidenceTier() {
        ArchitectureEntity fileModule = new ArchitectureEntity(
            "module:file:orderservice",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "src/main/java/com/example/orders/OrderService.java",
            "OrderService.java",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "file", "relativePath", "src/main/java/com/example/orders/OrderService.java")
        );
        ArchitectureEntity importedModule = new ArchitectureEntity(
            "module:file:requestcontext",
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            "org.springframework.web.context.request.RequestContext",
            "RequestContext",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "org.springframework.web.context.request.RequestContext")
        );
        ArchitectureRelationship importEvidence = new ArchitectureRelationship(
            "rel:import-evidence",
            RelationshipKind.DEPENDS_ON,
            fileModule.id(),
            importedModule.id(),
            "org.springframework.web.context.request.RequestContext",
            List.of(),
            Map.of("dependencySource", "import")
        );

        Map<String, Object> metadata = ArchitectureIrDependencyEvidenceEnricher.enrich(
            importEvidence,
            fileModule,
            importedModule,
            new LinkedHashMap<>(Map.of("dependencySource", "import"))
        );

        assertEquals("evidence", metadata.get("dependencyView"));
        assertEquals("supporting-evidence", metadata.get("dependencyTier"));
        assertTrue(((List<?>) metadata.get("dependencySources")).contains("import"));
    }
}
