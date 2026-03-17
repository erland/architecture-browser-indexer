package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasEndpoint;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasRelationshipByLabel;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasRelationshipKind;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertObservesEvent;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertPublishesEvent;
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

        assertHasEndpoint(resourceCreateOrder, "POST", "/orders");

        assertEquals(Boolean.TRUE, serviceCreateOrder.metadata().get("cdiEventPublisher"));
        assertEquals("com.example.orders.events.OrderCreatedEvent", serviceCreateOrder.metadata().get("cdiPublishedEventType"));
        assertTrue(extraction.relationships().stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "writePath".equals(rel.metadata().get("relationshipType"))
                    && "persist".equals(rel.metadata().get("writeOperation"))
                    && "com.example.orders.domain.OrderEntity".equals(rel.metadata().get("entityType"))),
            () -> "Expected write-path relationship for OrderEntity persist operation. Relationships=" + extraction.relationships());

        assertEquals(Boolean.TRUE, auditObserver.metadata().get("cdiObserver"));
        assertEquals(Boolean.FALSE, auditObserver.metadata().get("observerAsync"));
        assertEquals(Boolean.TRUE, asyncObserver.metadata().get("cdiObserver"));
        assertEquals(Boolean.TRUE, asyncObserver.metadata().get("observerAsync"));

        assertHasRelationshipByLabel(extraction.relationships(), RelationshipKind.EXPOSES, "POST /orders");
        assertPublishesEvent(extraction.relationships(), "com.example.orders.events.OrderCreatedEvent", "createOrder");
        assertObservesEvent(extraction.relationships(), "com.example.orders.events.OrderCreatedEvent", "onOrderCreated", null);
        assertObservesEvent(extraction.relationships(), "com.example.orders.events.OrderCreatedEvent", "onOrderCreatedAsync", Boolean.TRUE);
        assertHasRelationshipKind(extraction.relationships(), RelationshipKind.DEPENDS_ON, "hasAssociation");
        assertHasRelationshipKind(extraction.relationships(), RelationshipKind.DEPENDS_ON, "embeds");
    }

    private static ExtractedEntityFact methodByFile(StructuralExtractionResult result, String path, String name) {
        return result.entities().stream()
            .filter(entity -> "FUNCTION".equals(entity.kind().name()))
            .filter(entity -> name.equals(entity.name()))
            .filter(entity -> entity.sourceRefs().stream().anyMatch(ref -> path.equals(ref.path())))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing method " + path + "#" + name + ". Entities=" + result.entities()));
    }
}
