package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStageEndToEndFixtureRegressionTest {

    @Test
    void realisticJavaBackendFixturePreservesJavaStageArchitectSemanticsEndToEnd() {
        StructuralExtractionResult extraction = JavaBackendArchitectureFixtureTestData.buildExtractionFromFixture();

        assertTrue(extraction.entities().size() >= 20,
            () -> "Expected non-trivial Java extraction baseline. Entity count=" + extraction.entities().size());
        assertTrue(extraction.relationships().size() >= 20,
            () -> "Expected non-trivial Java extraction relationships. Relationship count=" + extraction.relationships().size());

        ExtractedEntityFact resourceCreateOrder = methodByFile(extraction, "src/main/java/com/example/orders/api/OrderResource.java", "createOrder");
        ExtractedEntityFact serviceCreateOrder = methodByFile(extraction, "src/main/java/com/example/orders/service/OrderService.java", "createOrder");
        ExtractedEntityFact repositorySave = methodByFile(extraction, "src/main/java/com/example/orders/repo/OrderRepository.java", "save");
        ExtractedEntityFact auditObserver = methodByFile(extraction, "src/main/java/com/example/orders/events/OrderCreatedAuditObserver.java", "onOrderCreated");
        ExtractedEntityFact asyncObserver = methodByFile(extraction, "src/main/java/com/example/orders/events/OrderCreatedAsyncProjector.java", "onOrderCreatedAsync");

        assertEquals(Boolean.TRUE, resourceCreateOrder.metadata().get("jaxRsEndpoint"));
        assertEquals("POST", resourceCreateOrder.metadata().get("httpMethod"));
        assertEquals("/orders", resourceCreateOrder.metadata().get("path"));

        assertEquals(Boolean.TRUE, repositorySave.metadata().get("writePath"));
        assertEquals(Boolean.TRUE, serviceCreateOrder.metadata().get("cdiEventPublisher"));
        assertEquals("com.example.orders.events.OrderCreatedEvent", serviceCreateOrder.metadata().get("cdiPublishedEventType"));
        assertTrue(stringList(repositorySave.metadata().get("writeEntityTypes")).contains("com.example.orders.domain.OrderEntity"),
            () -> "Expected OrderRepository#save to capture write-path entity types. Metadata=" + repositorySave.metadata());

        assertEquals(Boolean.TRUE, auditObserver.metadata().get("cdiObserver"));
        assertEquals(Boolean.FALSE, auditObserver.metadata().get("observerAsync"));
        assertEquals(Boolean.TRUE, asyncObserver.metadata().get("cdiObserver"));
        assertEquals(Boolean.TRUE, asyncObserver.metadata().get("observerAsync"));

        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.EXPOSES
                    && "POST /orders".equals(rel.label())),
            () -> "Expected JAX-RS exposes relationship. Relationships=" + extraction.relationships());
        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "publishesEvent".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.events.OrderCreatedEvent".equals(rel.metadata().get("eventType"))
                    && "createOrder".equals(rel.metadata().get("publisherMethod"))),
            () -> "Expected CDI publish relationship. Relationships=" + extraction.relationships());
        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "eventObservedBy".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.events.OrderCreatedEvent".equals(rel.metadata().get("eventType"))
                    && "onOrderCreated".equals(rel.metadata().get("observerMethod"))),
            () -> "Expected synchronous CDI observer relationship. Relationships=" + extraction.relationships());
        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "eventObservedBy".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.events.OrderCreatedEvent".equals(rel.metadata().get("eventType"))
                    && "onOrderCreatedAsync".equals(rel.metadata().get("observerMethod"))
                    && Boolean.TRUE.equals(rel.metadata().get("observerAsync"))),
            () -> "Expected async CDI observer relationship. Relationships=" + extraction.relationships());
        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "hasAssociation".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.domain.CustomerEntity".equals(rel.label())),
            () -> "Expected JPA association relationship. Relationships=" + extraction.relationships());
        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "embeds".equals(rel.metadata().get("relationshipType"))
                    && "com.example.orders.domain.AddressValue".equals(rel.label())),
            () -> "Expected JPA embedded-value relationship. Relationships=" + extraction.relationships());
    }

    private static ExtractedEntityFact methodByFile(StructuralExtractionResult result, String path, String name) {
        return result.entities().stream()
            .filter(entity -> "FUNCTION".equals(entity.kind().name()))
            .filter(entity -> name.equals(entity.name()))
            .filter(entity -> entity.sourceRefs().stream().anyMatch(ref -> path.equals(ref.path())))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing method " + path + "#" + name + ". Entities=" + result.entities()));
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
