package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaExtractionSemanticsSupportSeamTest {

    @Test
    void preservesStableJavaSemanticsContractsAcrossCdiWritePathAndPropertyHelpers() {
        assertEquals("persist", JavaExtractionSemanticsSupport.normalizeWriteOperation("save"));
        assertEquals("merge", JavaExtractionSemanticsSupport.normalizeWriteOperation("merge"));
        assertEquals("customerId", JavaExtractionSemanticsSupport.deriveJavaPropertyName("getCustomerId", "()"));

        var published = JavaExtractionSemanticsSupport.detectCdiPublishedEvents(
            "orderEvents.fireAsync(new OrderCreatedEvent(orderId));",
            "class OrderService { @Inject Event<OrderCreatedEvent> orderEvents; }"
        );
        assertEquals(1, published.size());
        assertEquals("OrderCreatedEvent", published.getFirst().eventType());
        assertTrue(published.getFirst().async());

        ExtractedEntityFact observerMethod = new ExtractedEntityFact(
            "entity:method:onOrderCreated",
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            "onOrderCreated",
            "onOrderCreated",
            "scope:file:test",
            List.of(),
            Map.of("parameters", "(@Critical @ObservesAsync OrderCreatedEvent event)")
        );
        var observed = JavaExtractionSemanticsSupport.detectCdiObservedEvent(observerMethod, "").orElseThrow();
        assertEquals("OrderCreatedEvent", observed.eventType());
        assertTrue(observed.async());
        assertTrue(observed.qualifiers().contains("Critical"));

        ExtractedEntityFact writePathMethod = new ExtractedEntityFact(
            "entity:method:createOrder",
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            "createOrder",
            "createOrder",
            "scope:file:test",
            List.of(),
            Map.of("parameters", "(OrderEntity order)", "parameterTypes", List.of("OrderEntity"))
        );
        Map<String, String> variableTypes = JavaExtractionSemanticsSupport.collectMethodVariableTypes(
            writePathMethod,
            "OrderEntity saved = repository.save(order); entityManager.merge(saved);"
        );
        assertEquals("OrderEntity", variableTypes.get("order"));
        assertEquals("OrderEntity", variableTypes.get("saved"));
        assertEquals("OrderEntity", JavaExtractionSemanticsSupport.resolveWriteTargetEntityType("saved", variableTypes).orElseThrow());
    }
}
