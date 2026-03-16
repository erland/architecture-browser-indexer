package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpretationHelperClassificationTest {

    @Test
    void javaBackendRoleClassifierKeepsServiceAndRepositoryProfilesStable() {
        SourceReference serviceRef = new SourceReference("src/main/java/com/example/orders/service/OrderService.java", 3, 8, "@ApplicationScoped class OrderService {}", Map.of());
        SourceReference repoRef = new SourceReference("src/main/java/com/example/orders/repo/OrderRepository.java", 3, 8, "class OrderRepository {}", Map.of());
        SourceReference fieldRef = new SourceReference("src/main/java/com/example/orders/service/OrderService.java", 4, 4, "@Inject OrderRepository orderRepository;", Map.of());

        ExtractedEntityFact service = new ExtractedEntityFact("entity:java:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "com.example.orders.service.OrderService", "scope:pkg", List.of(serviceRef), Map.of(
            "language", "java",
            "qualifiedName", "com.example.orders.service.OrderService",
            "packageName", "com.example.orders.service",
            "declarationKind", "class",
            "annotations", List.of("ApplicationScoped")
        ));
        ExtractedEntityFact repository = new ExtractedEntityFact("entity:java:repo", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderRepository", "com.example.orders.repo.OrderRepository", "scope:pkg", List.of(repoRef), Map.of(
            "language", "java",
            "qualifiedName", "com.example.orders.repo.OrderRepository",
            "packageName", "com.example.orders.repo",
            "declarationKind", "class",
            "annotations", List.of()
        ));
        ExtractedEntityFact injectedField = new ExtractedEntityFact("entity:java:field", EntityKind.FIELD, EntityOrigin.OBSERVED, "orderRepository", "orderRepository", "scope:pkg", List.of(fieldRef), Map.of(
            "language", "java",
            "ownerQualifiedName", "com.example.orders.service.OrderService",
            "declaredType", "OrderRepository",
            "annotations", List.of("Inject")
        ));

        InterpretationContext context = new InterpretationContext(new StructuralExtractionResult(
            List.of(),
            List.of(service, repository, injectedField),
            List.of(new ExtractedRelationshipFact("rel:service:repo", RelationshipKind.DEPENDS_ON, service.id(), repository.id(), repository.name(), List.of(fieldRef), Map.of("language", "java", "dependencySource", "field"))),
            List.of(),
            new ExtractionSummary(1, 1, Map.of("java", 1), Map.of("SYNTAX_TREE", 1), 3, 1)
        ));

        JavaBackendRoleClassifier classifier = new JavaBackendRoleClassifier();
        JavaBackendInterpretationClassification serviceClassification = classifier.classifyRole(service, context);
        JavaBackendInterpretationClassification repositoryClassification = classifier.classifyRole(repository, context);

        assertEquals(EntityKind.SERVICE, serviceClassification.roleKind());
        assertTrue(String.valueOf(serviceClassification.metadata().get("frameworks")).contains("cdi"));
        assertEquals(EntityKind.PERSISTENCE_ADAPTER, repositoryClassification.roleKind());
        assertEquals("repository", repositoryClassification.metadata().get("backendProfile"));
    }

    @Test
    void typescriptFrontendClassifierKeepsUiAndServiceProfilesStable() {
        TypeScriptFrontendClassifier classifier = new TypeScriptFrontendClassifier();

        ExtractedEntityFact component = new ExtractedEntityFact("entity:ts:component", EntityKind.CLASS, EntityOrigin.OBSERVED, "AppComponent", "src/app/app.component.ts#AppComponent", "scope:file", List.of(
            new SourceReference("src/app/app.component.ts", 1, 1, "@Component export class AppComponent {}", Map.of())
        ), Map.of("language", "typescript", "declarationKind", "class", "decorators", List.of("Component")));
        ExtractedEntityFact service = new ExtractedEntityFact("entity:ts:service", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrdersClient", "src/app/services/orders-client.ts#OrdersClient", "scope:file", List.of(
            new SourceReference("src/app/services/orders-client.ts", 1, 1, "export class OrdersClient { fetch('/api/orders'); }", Map.of())
        ), Map.of("language", "typescript", "declarationKind", "class", "decorators", List.of()));
        ExtractedEntityFact startup = new ExtractedEntityFact("entity:ts:startup", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "bootstrapApplication", "src/main.ts#bootstrapApplication", "scope:file", List.of(
            new SourceReference("src/main.ts", 1, 1, "export function bootstrapApplication() {}", Map.of())
        ), Map.of("language", "typescript", "declarationKind", "function", "decorators", List.of()));

        assertEquals("angular-component", classifier.classifyUiProfile(component));
        assertEquals("api-client-or-service", classifier.classifyServiceProfile(service));
        assertTrue(classifier.isStartupPoint(startup));
    }
}
