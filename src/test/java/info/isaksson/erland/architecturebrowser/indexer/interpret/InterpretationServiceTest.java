package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpretationServiceTest {

    @Test
    void infersJavaEndpointsServicesAndPersistenceAdapters() {
        SourceReference controllerRef = new SourceReference("src/main/java/com/example/DemoController.java", 5, 5, "@RestController class DemoController", Map.of());
        SourceReference methodRef = new SourceReference("src/main/java/com/example/DemoController.java", 12, 12, "@GetMapping(\"/orders\") public List<Order> getOrders()", Map.of());
        SourceReference serviceRef = new SourceReference("src/main/java/com/example/BillingService.java", 3, 3, "@Service class BillingService", Map.of());
        SourceReference repoRef = new SourceReference("src/main/java/com/example/OrderRepository.java", 4, 4, "@Repository interface OrderRepository", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:file:controller", EntityKind.MODULE, EntityOrigin.OBSERVED, "src/main/java/com/example/DemoController.java", "src/main/java/com/example/DemoController.java", "scope:file", List.of(controllerRef), Map.of("language", "java", "relativePath", "src/main/java/com/example/DemoController.java")),
                new ExtractedEntityFact("entity:java:controller", EntityKind.CLASS, EntityOrigin.OBSERVED, "DemoController", "com.example.DemoController", "scope:file", List.of(controllerRef), Map.of("language", "java", "annotations", List.of("RestController"))),
                new ExtractedEntityFact("entity:java:method:getOrders", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "getOrders", "getOrders()", "scope:file", List.of(methodRef), Map.of("language", "java", "annotations", List.of("GetMapping"))),
                new ExtractedEntityFact("entity:java:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "BillingService", "com.example.BillingService", "scope:file", List.of(serviceRef), Map.of("language", "java", "annotations", List.of("Service"))),
                new ExtractedEntityFact("entity:java:repo", EntityKind.INTERFACE, EntityOrigin.OBSERVED, "OrderRepository", "com.example.OrderRepository", "scope:file", List.of(repoRef), Map.of("language", "java", "annotations", List.of("Repository")))
            ),
            List.of(),
            List.of(),
            new ExtractionSummary(3, 3, Map.of("java", 3), Map.of("SYNTAX_TREE", 3), 4, 4)
        );

        InterpretationResult result = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.ENDPOINT && entity.name().contains("GET /orders")));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE && "BillingService".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.PERSISTENCE_ADAPTER && "OrderRepository".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXPOSES && rel.label().contains("GET /orders")));
    }

    @Test
    void infersTypeScriptUiModulesServicesAndStartupPoints() {
        SourceReference componentRef = new SourceReference("src/app/app.component.ts", 4, 4, "@Component export class AppComponent {}", Map.of());
        SourceReference serviceRef = new SourceReference("src/app/api.service.ts", 2, 2, "@Injectable export class ApiService {}", Map.of());
        SourceReference startupRef = new SourceReference("src/main.ts", 1, 1, "export function bootstrapApplication() {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:ts:component", EntityKind.CLASS, EntityOrigin.OBSERVED, "AppComponent", "src/app/app.component.ts#AppComponent", "scope:file", List.of(componentRef), Map.of("language", "typescript", "decorators", List.of("Component"))),
                new ExtractedEntityFact("entity:ts:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "ApiService", "src/app/api.service.ts#ApiService", "scope:file", List.of(serviceRef), Map.of("language", "typescript", "decorators", List.of("Injectable"))),
                new ExtractedEntityFact("entity:ts:startup", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "bootstrapApplication", "src/main.ts#bootstrapApplication", "scope:file", List.of(startupRef), Map.of("language", "typescript", "decorators", List.of()))
            ),
            List.of(),
            List.of(),
            new ExtractionSummary(3, 3, Map.of("typescript", 3), Map.of("SYNTAX_TREE", 3), 3, 3)
        );

        InterpretationResult result = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "AppComponent".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE && "ApiService".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.STARTUP_POINT && "bootstrapApplication".equals(entity.name())));
    }
}
