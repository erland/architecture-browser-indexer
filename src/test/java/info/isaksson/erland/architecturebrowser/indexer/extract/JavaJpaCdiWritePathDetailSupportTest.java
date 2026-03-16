package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaJpaCdiWritePathDetailSupportTest {
    @Test void jpaHelperExtractsAssociationMetadata() {
        JavaJpaDetailSupport support = new JavaJpaDetailSupport();
        ExtractedEntityFact field = new ExtractedEntityFact("f", EntityKind.FIELD, EntityOrigin.OBSERVED, "customer", "customer", "scope:file", List.of(new SourceReference("x", 1, 1, "@ManyToOne @JoinColumn(name = \"customer_id\") private Customer customer;", Map.of())), Map.of("annotations", List.of("ManyToOne"), "declaredType", "Customer"));
        var details = support.analyzeField(field, field.sourceRefs().getFirst().snippet());
        assertEquals("many-to-one", details.associationKind());
        assertEquals("customer_id", details.joinColumn());
    }
    @Test void cdiHelperDetectsPublisherAndObserver() {
        JavaCdiDetailSupport support = new JavaCdiDetailSupport();
        assertTrue(support.detectPublishedEvents("events.fire(new OrderCreated(id));", "class A { @Inject Event<OrderCreated> events; }").stream().anyMatch(p -> p.publisherField().equals("events")));
        ExtractedEntityFact method = new ExtractedEntityFact("m", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "onOrderCreated", "onOrderCreated", "scope:file", List.of(), Map.of("parameters", "(@ObservesAsync @Critical OrderCreated event)"));
        var observed = support.detectObservedEvent(method, "").orElseThrow();
        assertEquals("OrderCreated", observed.eventType());
        assertTrue(observed.async());
    }
    @Test void writePathHelperDetectsCalls() {
        JavaWritePathDetailSupport support = new JavaWritePathDetailSupport();
        ExtractedEntityFact method = new ExtractedEntityFact("m2", EntityKind.FUNCTION, EntityOrigin.OBSERVED, "createOrder", "createOrder", "scope:file", List.of(), Map.of("parameters", "(Order order)", "parameterTypes", List.of("Order")));
        assertEquals(2, support.detectWritePaths(method, "Order saved = orderRepository.save(order); entityManager.merge(saved);").size());
    }
}
