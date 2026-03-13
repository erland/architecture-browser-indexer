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
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:order", ScopeKind.PACKAGE, "com.example.order", "com.example.order", "scope:repo", List.of(aRef), Map.of("language", "java")),
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope("scope:pkg:shared", ScopeKind.PACKAGE, "com.example.shared", "com.example.shared", "scope:repo", List.of(bRef), Map.of("language", "java"))
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
        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.DIRECTORY && "src/main/java/com/example/order".equals(scope.name()) && "order".equals(scope.displayName())));
        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.MODULE && "src/main/java".equals(scope.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE && "src/main/java".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "com.example.shared.CustomerRepository".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && "com.example.order".equals(rel.label())));
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
}
