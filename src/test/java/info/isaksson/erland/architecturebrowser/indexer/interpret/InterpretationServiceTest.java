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

    @Test
    void usesOwnerQualifiedNameForJavaEndpointsAndFieldCollaborators() {
        SourceReference controllerRef = new SourceReference("src/main/java/com/example/OrderController.java", 3, 8, "@RequestMapping(\"/orders\") @RestController class OrderController {}", Map.of());
        SourceReference methodRef = new SourceReference("src/main/java/com/example/OrderController.java", 12, 12, "@GetMapping(\"/{id}\") public OrderDto getOrder(String id)", Map.of());
        SourceReference ownerRef = new SourceReference("src/main/java/com/example/OrderService.java", 2, 6, "class OrderService { private final CustomerRepository customerRepository; }", Map.of());
        SourceReference repoFieldRef = new SourceReference("src/main/java/com/example/OrderService.java", 4, 4, "private final CustomerRepository customerRepository;", Map.of());
        SourceReference repoRef = new SourceReference("src/main/java/com/example/CustomerRepository.java", 2, 2, "@Repository interface CustomerRepository {}", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:file:controller", EntityKind.MODULE, EntityOrigin.OBSERVED, "src/main/java/com/example/OrderController.java", "src/main/java/com/example/OrderController.java", "scope:file", List.of(controllerRef), Map.of("language", "java", "relativePath", "src/main/java/com/example/OrderController.java")),
                new ExtractedEntityFact("entity:java:controller", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderController", "com.example.OrderController", "scope:pkg", List.of(controllerRef), Map.of("language", "java", "qualifiedName", "com.example.OrderController", "declarationKind", "class", "annotations", List.of("RestController", "RequestMapping"))),
                new ExtractedEntityFact("entity:java:method:getOrder", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "getOrder", "getOrder(String id)", "scope:file", List.of(methodRef), Map.of("language", "java", "annotations", List.of("GetMapping"), "ownerQualifiedName", "com.example.OrderController")),
                new ExtractedEntityFact("entity:java:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "com.example.OrderService", "scope:pkg", List.of(ownerRef), Map.of("language", "java", "qualifiedName", "com.example.OrderService", "declarationKind", "class", "annotations", List.of("Service"))),
                new ExtractedEntityFact("entity:java:field:repo", EntityKind.FIELD, EntityOrigin.OBSERVED, "customerRepository", "customerRepository", "scope:file", List.of(repoFieldRef), Map.of("language", "java", "ownerQualifiedName", "com.example.OrderService", "declaredType", "CustomerRepository")),
                new ExtractedEntityFact("entity:java:repo", EntityKind.INTERFACE, EntityOrigin.OBSERVED, "CustomerRepository", "com.example.CustomerRepository", "scope:pkg", List.of(repoRef), Map.of("language", "java", "qualifiedName", "com.example.CustomerRepository", "declarationKind", "interface", "annotations", List.of("Repository")))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:service:repo", RelationshipKind.DEPENDS_ON, "entity:java:service", "entity:java:repo", "com.example.CustomerRepository", List.of(repoFieldRef), Map.of("language", "java", "dependencySource", "field"))
            ),
            List.of(),
            new ExtractionSummary(3, 3, Map.of("java", 3), Map.of("SYNTAX_TREE", 3), 6, 1)
        );

        InterpretationResult result = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.ENDPOINT && entity.name().contains("GET /orders/{id}")));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXPOSES && "entity:java:controller".equals(rel.fromEntityId()) && rel.label().contains("GET /orders/{id}")));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES && "entity:java:service".equals(rel.fromEntityId()) && "customerRepository".equals(rel.label()) && String.valueOf(rel.metadata().get("dependencySource")).equals("field")));
    }


    @Test
    void infersRicherTypeScriptUiAndServiceProfiles() {
        SourceReference angularComponentRef = new SourceReference("src/app/orders/order-list.component.ts", 4, 12, "@Component export class OrderListComponent {}", Map.of());
        SourceReference angularDirectiveRef = new SourceReference("src/app/shared/focus.directive.ts", 2, 6, "@Directive export class FocusDirective {}", Map.of());
        SourceReference reactComponentRef = new SourceReference("src/components/UserCard.tsx", 1, 6, "export function UserCard() { return <div />; }", Map.of());
        SourceReference reactPageRef = new SourceReference("src/pages/OrdersPage.tsx", 1, 6, "export function OrdersPage() { return <main />; }", Map.of());
        SourceReference contextProviderRef = new SourceReference("src/context/AuthProvider.tsx", 1, 8, "export function AuthProvider() { const value = React.createContext(null); return <AuthContext.Provider value={value} />; }", Map.of());
        SourceReference apiClientRef = new SourceReference("src/api/orders.client.ts", 1, 6, "export function OrdersClient() { return fetch('/api/orders'); }", Map.of());
        SourceReference stateRef = new SourceReference("src/state/session.store.ts", 1, 6, "export function sessionStore() { return {}; }", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:ts:angular:component", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderListComponent", "src/app/orders/order-list.component.ts#OrderListComponent", "scope:file", List.of(angularComponentRef), Map.of("language", "typescript", "declarationKind", "class", "decorators", List.of("Component"))),
                new ExtractedEntityFact("entity:ts:angular:directive", EntityKind.CLASS, EntityOrigin.OBSERVED, "FocusDirective", "src/app/shared/focus.directive.ts#FocusDirective", "scope:file", List.of(angularDirectiveRef), Map.of("language", "typescript", "declarationKind", "class", "decorators", List.of("Directive"))),
                new ExtractedEntityFact("entity:ts:react:component", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "UserCard", "src/components/UserCard.tsx#UserCard", "scope:file", List.of(reactComponentRef), Map.of("language", "typescript", "declarationKind", "function", "decorators", List.of())),
                new ExtractedEntityFact("entity:ts:react:page", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "OrdersPage", "src/pages/OrdersPage.tsx#OrdersPage", "scope:file", List.of(reactPageRef), Map.of("language", "typescript", "declarationKind", "function", "decorators", List.of())),
                new ExtractedEntityFact("entity:ts:react:provider", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "AuthProvider", "src/context/AuthProvider.tsx#AuthProvider", "scope:file", List.of(contextProviderRef), Map.of("language", "typescript", "declarationKind", "function", "decorators", List.of())),
                new ExtractedEntityFact("entity:ts:service:api", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "OrdersClient", "src/api/orders.client.ts#OrdersClient", "scope:file", List.of(apiClientRef), Map.of("language", "typescript", "declarationKind", "function", "decorators", List.of())),
                new ExtractedEntityFact("entity:ts:service:state", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "sessionStore", "src/state/session.store.ts#sessionStore", "scope:file", List.of(stateRef), Map.of("language", "typescript", "declarationKind", "function", "decorators", List.of()))
            ),
            List.of(),
            List.of(),
            new ExtractionSummary(7, 7, Map.of("typescript", 7), Map.of("SYNTAX_TREE", 7), 7, 7)
        );

        InterpretationResult result = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "OrderListComponent".equals(entity.name()) && "angular-component".equals(entity.metadata().get("uiProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "FocusDirective".equals(entity.name()) && "angular-directive".equals(entity.metadata().get("uiProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "UserCard".equals(entity.name()) && "react-function-component".equals(entity.metadata().get("uiProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "OrdersPage".equals(entity.name()) && "page-or-router".equals(entity.metadata().get("uiProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "AuthProvider".equals(entity.name()) && "react-context".equals(entity.metadata().get("uiProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE && "OrdersClient".equals(entity.name()) && "api-client-or-service".equals(entity.metadata().get("serviceProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE && "sessionStore".equals(entity.name()) && "state-module".equals(entity.metadata().get("serviceProfile"))));
    }

    @Test
    void infersJavaBackendRoleMetadataFromFrameworksPackagesAndDependencies() {
        SourceReference resourceRef = new SourceReference("src/main/java/com/example/orders/api/OrderResource.java", 3, 8, "@Path(\"/orders\") class OrderResource", Map.of());
        SourceReference serviceRef = new SourceReference("src/main/java/com/example/orders/service/OrderService.java", 3, 12, "@ApplicationScoped class OrderService", Map.of());
        SourceReference repoRef = new SourceReference("src/main/java/com/example/orders/repo/OrderRepository.java", 3, 8, "class OrderRepository", Map.of());
        SourceReference eventRef = new SourceReference("src/main/java/com/example/orders/service/OrderService.java", 14, 18, "orderCreatedEvents.fire(new OrderCreatedEvent(id));", Map.of());

        StructuralExtractionResult extraction = new StructuralExtractionResult(
            List.of(),
            List.of(
                new ExtractedEntityFact("entity:java:resource", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderResource", "com.example.orders.api.OrderResource", "scope:pkg", List.of(resourceRef), Map.of("language", "java", "qualifiedName", "com.example.orders.api.OrderResource", "packageName", "com.example.orders.api", "declarationKind", "class", "annotations", List.of("Path"), "jaxRsResource", true)),
                new ExtractedEntityFact("entity:java:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "com.example.orders.service.OrderService", "scope:pkg", List.of(serviceRef), Map.of("language", "java", "qualifiedName", "com.example.orders.service.OrderService", "packageName", "com.example.orders.service", "declarationKind", "class", "annotations", List.of("ApplicationScoped"))),
                new ExtractedEntityFact("entity:java:repo", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderRepository", "com.example.orders.repo.OrderRepository", "scope:pkg", List.of(repoRef), Map.of("language", "java", "qualifiedName", "com.example.orders.repo.OrderRepository", "packageName", "com.example.orders.repo", "declarationKind", "class", "annotations", List.of("ApplicationScoped"))),
                new ExtractedEntityFact("entity:java:service:publisher", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "createOrder", "createOrder(String id)", "scope:pkg", List.of(eventRef), Map.of("language", "java", "ownerQualifiedName", "com.example.orders.service.OrderService", "cdiEventPublisher", true, "cdiPublishedEventType", "com.example.orders.events.OrderCreatedEvent", "annotations", List.of())),
                new ExtractedEntityFact("entity:java:service:field", EntityKind.FIELD, EntityOrigin.OBSERVED, "orderRepository", "orderRepository", "scope:pkg", List.of(serviceRef), Map.of("language", "java", "ownerQualifiedName", "com.example.orders.service.OrderService", "declaredType", "OrderRepository", "annotations", List.of("Inject")))
            ),
            List.of(
                new ExtractedRelationshipFact("rel:service:repo", RelationshipKind.DEPENDS_ON, "entity:java:service", "entity:java:repo", "com.example.orders.repo.OrderRepository", List.of(serviceRef), Map.of("language", "java", "dependencySource", "field"))
            ),
            List.of(),
            new ExtractionSummary(3, 3, Map.of("java", 3), Map.of("SYNTAX_TREE", 3), 5, 1)
        );

        InterpretationResult result = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE
            && "OrderResource".equals(entity.name())
            && "resource".equals(entity.metadata().get("entityRole"))
            && "jax-rs-resource".equals(entity.metadata().get("backendProfile"))));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE
            && "OrderService".equals(entity.name())
            && "service".equals(entity.metadata().get("entityRole"))
            && String.valueOf(entity.metadata().get("backendProfile")).contains("application-service")
            && String.valueOf(entity.metadata().get("frameworks")).contains("cdi")));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.PERSISTENCE_ADAPTER
            && "OrderRepository".equals(entity.name())
            && "repository".equals(entity.metadata().get("entityRole"))
            && "repository".equals(entity.metadata().get("backendProfile"))));
    }

}
