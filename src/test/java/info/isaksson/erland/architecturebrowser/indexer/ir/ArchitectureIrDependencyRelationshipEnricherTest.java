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

class ArchitectureIrDependencyRelationshipEnricherTest {

    @Test
    void enrichesPackageAndModuleDependencyRelationshipsWithStableViewMetadata() {
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
            "source-root:main",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "src/main/java",
            "src/main/java",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "source-root")
        );
        ArchitectureEntity targetRoot = new ArchitectureEntity(
            "source-root:test",
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            "src/test/java",
            "src/test/java",
            "scope:repo",
            List.of(),
            Map.of("logicalRole", "source-root")
        );

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(sourcePackage.id(), sourcePackage);
        entitiesById.put(targetPackage.id(), targetPackage);
        entitiesById.put(sourceRoot.id(), sourceRoot);
        entitiesById.put(targetRoot.id(), targetRoot);

        ArchitectureRelationship packageDependency = new ArchitectureRelationship(
            "rel:pkg",
            RelationshipKind.USES,
            sourcePackage.id(),
            targetPackage.id(),
            "package uses",
            List.of(),
            Map.of("rollup", "package-package")
        );
        ArchitectureRelationship moduleDependency = new ArchitectureRelationship(
            "rel:module",
            RelationshipKind.USES,
            sourceRoot.id(),
            targetRoot.id(),
            "module uses",
            List.of(),
            Map.of("rollup", "module-module")
        );

        List<ArchitectureRelationship> enriched = ArchitectureIrDependencyRelationshipEnricher.enrichDependencyRelationshipMetadata(
            List.of(packageDependency, moduleDependency),
            entitiesById,
            Map.of()
        );

        ArchitectureRelationship enrichedPackageDependency = enriched.stream()
            .filter(relationship -> relationship.id().equals("rel:pkg"))
            .findFirst()
            .orElseThrow();
        ArchitectureRelationship enrichedModuleDependency = enriched.stream()
            .filter(relationship -> relationship.id().equals("rel:module"))
            .findFirst()
            .orElseThrow();

        assertEquals("package", enrichedPackageDependency.metadata().get("dependencyView"));
        assertEquals("com.example.api", enrichedPackageDependency.metadata().get("dependencySourcePackageName"));
        assertEquals("com.example.domain", enrichedPackageDependency.metadata().get("dependencyTargetPackageName"));
        assertEquals("internal", enrichedPackageDependency.metadata().get("dependencyTargetBoundary"));

        assertEquals("module", enrichedModuleDependency.metadata().get("dependencyView"));
        assertEquals("src/main/java", enrichedModuleDependency.metadata().get("dependencySourceModuleName"));
        assertEquals("src/test/java", enrichedModuleDependency.metadata().get("dependencyTargetModuleName"));
        assertEquals(Boolean.FALSE, enrichedModuleDependency.metadata().get("sameModule"));
        assertEquals("internal", enrichedModuleDependency.metadata().get("dependencyTargetBoundary"));
    }

    @Test
    void preservesImportEvidenceMetadataAsEvidenceTier() {
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

        List<ArchitectureRelationship> enriched = ArchitectureIrDependencyRelationshipEnricher.enrichDependencyRelationshipMetadata(
            List.of(importEvidence),
            Map.of(fileModule.id(), fileModule, importedModule.id(), importedModule),
            Map.of()
        );

        ArchitectureRelationship evidence = enriched.getFirst();
        assertEquals("evidence", evidence.metadata().get("dependencyView"));
        assertEquals("supporting-evidence", evidence.metadata().get("dependencyTier"));
        assertTrue(((List<?>) evidence.metadata().get("dependencySources")).contains("import"));
    }
}
