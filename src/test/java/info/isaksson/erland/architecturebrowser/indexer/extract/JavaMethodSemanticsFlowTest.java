package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class JavaMethodSemanticsFlowTest {

    @Test
    void appliesJaxRsJpaCdiAndWritePathSemanticsThroughDedicatedHelpers() {
        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderResource.java",
            "package com.example.orders; import jakarta.persistence.Entity; @Path(\"/orders\") class OrderResource { Event<OrderCreated> events; @POST OrderEntity create(OrderEntity request) { events.fire(new OrderCreated()); repository.save(request); return new OrderEntity(); } } @Entity class OrderEntity {}",
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(
                1,
                "OrderResource",
                "@Path(\"/orders\") class OrderResource { Event<OrderCreated> events; @POST OrderEntity create(OrderEntity request) { events.fire(new OrderCreated()); repository.save(request); return new OrderEntity(); } }",
                annotation(1, "@Path(\"/orders\")"),
                fieldDecl(1, "Event<OrderCreated> events;", "Event<OrderCreated>", "events"),
                fieldDecl(1, "OrderRepository repository;", "OrderRepository", "repository"),
                methodDecl(1, "@POST OrderEntity create(OrderEntity request) { events.fire(new OrderCreated()); repository.save(request); return new OrderEntity(); }", "OrderEntity", "create", "(OrderEntity request)", annotation(1, "@POST"))
            ),
            classDecl(2, "OrderEntity", "@Entity class OrderEntity {}", annotation(2, "@Entity"))
        );

        var method = methodByOwner(accumulator, "com.example.orders.OrderResource", "create");
        assertEquals(Boolean.TRUE, method.metadata().get("jaxRsEndpoint"));
        assertEquals("com.example.orders.OrderCreated", method.metadata().get("cdiPublishedEventType"));
        assertEquals(Boolean.TRUE, method.metadata().get("writePath"));
    }
}
