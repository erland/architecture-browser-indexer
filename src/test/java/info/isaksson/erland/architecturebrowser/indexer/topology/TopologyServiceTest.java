package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationSummary;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyServiceTest {

    @Test
    void infersDirectoryAndModuleScopesAndInternalRelationships() {
        String aPath = "src/main/java/com/example/order/OrderService.java";
        String bPath = "src/main/java/com/example/shared/CustomerRepository.java";
        SourceReference aRef = new SourceReference(aPath, 5, 5, "class OrderService {}", Map.of());
        SourceReference bRef = new SourceReference(bPath, 4, 4, "interface CustomerRepository {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:example", ScopeKind.PACKAGE, "com.example", "example", "scope:repo", List.of(aRef, bRef), Map.of("language", "java")),
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:order", ScopeKind.PACKAGE, "com.example.order", "com.example.order", "scope:pkg:example", List.of(aRef), Map.of("language", "java")),
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:shared", ScopeKind.PACKAGE, "com.example.shared", "com.example.shared", "scope:pkg:example", List.of(bRef), Map.of("language", "java"))
            ),
            List.of(
                new ExtractedEntityFact("entity:file:a", EntityKind.MODULE, EntityOrigin.OBSERVED, aPath, aPath, "scope:file:a", List.of(aRef), Map.of("language", "java", "relativePath", aPath)),
                new ExtractedEntityFact("entity:file:b", EntityKind.MODULE, EntityOrigin.OBSERVED, bPath, bPath, "scope:file:b", List.of(bRef), Map.of("language", "java", "relativePath", bPath)),
                new ExtractedEntityFact("entity:class:a", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "com.example.order.OrderService", "scope:pkg:order", List.of(aRef), Map.of("language", "java", "qualifiedName", "com.example.order.OrderService")),
                new ExtractedEntityFact("entity:class:b", EntityKind.INTERFACE, EntityOrigin.OBSERVED, "CustomerRepository", "com.example.shared.CustomerRepository", "scope:pkg:shared", List.of(bRef), Map.of("language", "java", "qualifiedName", "com.example.shared.CustomerRepository"))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:dep:a", RelationshipKind.DEPENDS_ON, "entity:file:a", "entity:external:java:x", "com.example.shared.CustomerRepository", List.of(aRef), Map.of("language", "java"))
            ),
            List.of(),
            new ExtractionSummary(2, 2, Map.of("java", 2), Map.of("SYNTAX_TREE", 2), 4, 1)
        );

        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry(aPath, 100, "java", "source", "java", false, List.of("spring")),
                new FileInventoryEntry(bPath, 100, "java", "source", "java", false, List.of("spring"))
            ),
            2, 2, 0, Set.of("java"), Set.of("spring")
        );

        TopologyService service = new TopologyService();
        var result = service.infer(inventory, extraction, new InterpretationResult(List.of(), List.of(), List.of(), new InterpretationSummary(Map.of(), Map.of(), Map.of())));

        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.DIRECTORY && "src/main/java/com/example/order".equals(scope.name())));
        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.MODULE && "src/main/java".equals(scope.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE && "src/main/java".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "com.example.shared.CustomerRepository".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && "com.example.order".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && "com.example.shared".equals(rel.label())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE && "order".equals(entity.displayName()) && "com.example.order".equals(entity.name())));
    }

    @Test
    void resolvesTypescriptRelativeImportsToInternalFileModules() {
        String fromPath = "src/main/ts/app/main.ts";
        String toPath = "src/main/ts/app/http-client.ts";
        SourceReference fromRef = new SourceReference(fromPath, 2, 2, "import { x } from './http-client';", Map.of());
        SourceReference toRef = new SourceReference(toPath, 1, 1, "export class HttpClient {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:file:from", EntityKind.MODULE, EntityOrigin.OBSERVED, fromPath, fromPath, "scope:file:from", List.of(fromRef), Map.of("language", "typescript", "relativePath", fromPath)),
                new ExtractedEntityFact("entity:file:to", EntityKind.MODULE, EntityOrigin.OBSERVED, toPath, toPath, "scope:file:to", List.of(toRef), Map.of("language", "typescript", "relativePath", toPath))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:dep:ts", RelationshipKind.DEPENDS_ON, "entity:file:from", "entity:external:ts:http", "./http-client", List.of(fromRef), Map.of("language", "typescript"))
            ),
            List.of(),
            new ExtractionSummary(2, 2, Map.of("typescript", 2), Map.of("SYNTAX_TREE", 2), 2, 1)
        );

        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry(fromPath, 100, "ts", "source", "typescript", false, List.of("react")),
                new FileInventoryEntry(toPath, 100, "ts", "source", "typescript", false, List.of("react"))
            ),
            2, 2, 0, Set.of("typescript"), Set.of("react")
        );

        var result = new TopologyService().infer(inventory, extraction, new InterpretationResult(List.of(), List.of(), List.of(), new InterpretationSummary(Map.of(), Map.of(), Map.of())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "./http-client".equals(rel.label())));
    }

    @Test
    void rollsUpJavaTypeRelationshipsAcrossPackagesAndModules() {
        String servicePath = "src/main/java/com/example/order/OrderService.java";
        String repoPath = "src/main/java/com/example/shared/CustomerRepository.java";
        SourceReference serviceRef = new SourceReference(servicePath, 5, 5, "class OrderService extends BaseService implements CustomerRepository {}", Map.of());
        SourceReference repoRef = new SourceReference(repoPath, 4, 4, "interface CustomerRepository {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:order", ScopeKind.PACKAGE, "com.example.order", "com.example.order", "scope:repo", List.of(serviceRef), Map.of("language", "java")),
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:shared", ScopeKind.PACKAGE, "com.example.shared", "com.example.shared", "scope:repo", List.of(repoRef), Map.of("language", "java"))
            ),
            List.of(
                new ExtractedEntityFact("entity:file:service", EntityKind.MODULE, EntityOrigin.OBSERVED, servicePath, servicePath, "scope:file:service", List.of(serviceRef), Map.of("language", "java", "relativePath", servicePath)),
                new ExtractedEntityFact("entity:file:repo", EntityKind.MODULE, EntityOrigin.OBSERVED, repoPath, repoPath, "scope:file:repo", List.of(repoRef), Map.of("language", "java", "relativePath", repoPath)),
                new ExtractedEntityFact("entity:class:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "com.example.order.OrderService", "scope:pkg:order", List.of(serviceRef), Map.of("language", "java", "qualifiedName", "com.example.order.OrderService", "declarationKind", "class")),
                new ExtractedEntityFact("entity:interface:repo", EntityKind.INTERFACE, EntityOrigin.OBSERVED, "CustomerRepository", "com.example.shared.CustomerRepository", "scope:pkg:shared", List.of(repoRef), Map.of("language", "java", "qualifiedName", "com.example.shared.CustomerRepository", "declarationKind", "interface"))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:implements", RelationshipKind.IMPLEMENTS, "entity:class:service", "entity:interface:repo", "com.example.shared.CustomerRepository", List.of(serviceRef), Map.of("language", "java")),
                new ExtractedRelationshipFact("rel:depends", RelationshipKind.DEPENDS_ON, "entity:class:service", "entity:interface:repo", "com.example.shared.CustomerRepository", List.of(serviceRef), Map.of("language", "java", "dependencySource", "field"))
            ),
            List.of(),
            new ExtractionSummary(2, 2, Map.of("java", 2), Map.of("SYNTAX_TREE", 2), 4, 2)
        );

        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry(servicePath, 100, "java", "source", "java", false, List.of("spring")),
                new FileInventoryEntry(repoPath, 100, "java", "source", "java", false, List.of("spring"))
            ),
            2, 2, 0, Set.of("java"), Set.of("spring")
        );

        var result = new TopologyService().infer(inventory, extraction, new InterpretationResult(List.of(), List.of(), List.of(), new InterpretationSummary(Map.of(), Map.of(), Map.of())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS && "entity:class:service".equals(rel.fromEntityId()) && "entity:interface:repo".equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS && "package-package".equals(rel.metadata().get("rollup"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "module-module".equals(rel.metadata().get("rollup"))));
    }

    @Test
    void rollsUpTypeScriptTypeDependenciesAcrossPackagesAndSourceRoots() {
        String pagePath = "src/app/pages/OrdersPage.tsx";
        String servicePath = "src/services/core/OrderService.ts";
        SourceReference pageRef = new SourceReference(pagePath, 3, 3, "export function OrdersPage() { return null; }", Map.of());
        SourceReference serviceRef = new SourceReference(servicePath, 1, 1, "export class OrderService {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:file:page", EntityKind.MODULE, EntityOrigin.OBSERVED, pagePath, pagePath, "scope:file:page", List.of(pageRef), Map.of("language", "typescript", "relativePath", pagePath)),
                new ExtractedEntityFact("entity:file:service", EntityKind.MODULE, EntityOrigin.OBSERVED, servicePath, servicePath, "scope:file:service", List.of(serviceRef), Map.of("language", "typescript", "relativePath", servicePath)),
                new ExtractedEntityFact("entity:type:page", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "OrdersPage", "OrdersPage", "scope:file:page", List.of(pageRef), Map.of("language", "typescript", "qualifiedName", "OrdersPage", "declarationKind", "function")),
                new ExtractedEntityFact("entity:type:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "OrderService", "scope:file:service", List.of(serviceRef), Map.of("language", "typescript", "qualifiedName", "OrderService", "declarationKind", "class"))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:dep:typed", RelationshipKind.DEPENDS_ON, "entity:type:page", "entity:type:service", "OrderService", List.of(pageRef), Map.of("language", "typescript", "dependencySource", "returnType", "targetClassification", "observed-source-type")),
                new ExtractedRelationshipFact("rel:dep:import", RelationshipKind.DEPENDS_ON, "entity:file:page", "entity:external:ts:service", "../../services/core/OrderService", List.of(pageRef), Map.of("language", "typescript", "dependencySource", "import", "targetClassification", "inferred-internal-type"))
            ),
            List.of(),
            new ExtractionSummary(2, 2, Map.of("typescript", 2), Map.of("SYNTAX_TREE", 2), 4, 2)
        );

        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry(pagePath, 100, "tsx", "source", "typescript", false, List.of("react")),
                new FileInventoryEntry(servicePath, 100, "ts", "source", "typescript", false, List.of("react"))
            ),
            2, 2, 0, Set.of("typescript"), Set.of("react")
        );

        var result = new TopologyService().infer(inventory, extraction, new InterpretationResult(List.of(), List.of(), List.of(), new InterpretationSummary(Map.of(), Map.of(), Map.of())));
        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.MODULE && "src/app".equals(scope.name())));
        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.MODULE && "src/services".equals(scope.name())));
        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.PACKAGE && "src/app/pages".equals(scope.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "package-package".equals(rel.metadata().get("rollup"))
            && rel.fromEntityId().equals(info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils.externalEntityId("logical-package", "src/app/pages"))
            && rel.toEntityId().equals(info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils.externalEntityId("logical-package", "src/services/core"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "module-module".equals(rel.metadata().get("rollup"))
            && rel.fromEntityId().equals(info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils.externalEntityId("logical-module", "src/app"))
            && rel.toEntityId().equals(info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils.externalEntityId("logical-module", "src/services"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "file-evidence".equals(rel.metadata().get("rollup"))
            && "../../services/core/OrderService".equals(rel.label())));
    }

    @Test
    void keepsTypeScriptImportDependenciesAsEvidenceNotPrimaryRollups() {
        String pagePath = "src/app/pages/OrdersPage.tsx";
        String servicePath = "src/services/core/OrderService.ts";
        SourceReference pageRef = new SourceReference(pagePath, 2, 2, "import { OrderService } from '../../services/core/OrderService';", Map.of());
        SourceReference serviceRef = new SourceReference(servicePath, 1, 1, "export class OrderService {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:file:page", EntityKind.MODULE, EntityOrigin.OBSERVED, pagePath, pagePath, "scope:file:page", List.of(pageRef), Map.of("language", "typescript", "relativePath", pagePath)),
                new ExtractedEntityFact("entity:file:service", EntityKind.MODULE, EntityOrigin.OBSERVED, servicePath, servicePath, "scope:file:service", List.of(serviceRef), Map.of("language", "typescript", "relativePath", servicePath))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:dep:import-only", RelationshipKind.DEPENDS_ON, "entity:file:page", "entity:file:service", "../../services/core/OrderService", List.of(pageRef), Map.of("language", "typescript", "dependencySource", "import"))
            ),
            List.of(),
            new ExtractionSummary(2, 2, Map.of("typescript", 2), Map.of("SYNTAX_TREE", 2), 2, 1)
        );

        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry(pagePath, 100, "tsx", "source", "typescript", false, List.of("react")),
                new FileInventoryEntry(servicePath, 100, "ts", "source", "typescript", false, List.of("react"))
            ),
            2, 2, 0, Set.of("typescript"), Set.of("react")
        );

        var result = new TopologyService().infer(inventory, extraction, new InterpretationResult(List.of(), List.of(), List.of(), new InterpretationSummary(Map.of(), Map.of(), Map.of())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "file-evidence".equals(rel.metadata().get("rollup"))));
        assertTrue(result.relationships().stream().noneMatch(rel -> "package-package".equals(rel.metadata().get("rollup"))));
        assertTrue(result.relationships().stream().noneMatch(rel -> "module-module".equals(rel.metadata().get("rollup"))));
    }

}
