package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaMethodExtractionFlowTest {

    @Test
    void extractsMethodEntitiesContainmentDependenciesAndMethodSemantics() {
        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderResource.java",
            "package com.example.orders; @Path(\"/orders\") class OrderResource { @POST OrderCreated create(OrderRequest request) { events.fire(new OrderCreated()); return new OrderCreated(); } }",
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(
                1,
                "OrderResource",
                "@Path(\"/orders\") class OrderResource { @POST OrderCreated create(OrderRequest request) { events.fire(new OrderCreated()); return new OrderCreated(); } }",
                annotation(1, "@Path(\"/orders\")"),
                fieldDecl(1, "Event<OrderCreated> events;", "Event<OrderCreated>", "events"),
                methodDecl(1, "@POST OrderCreated create(OrderRequest request) { events.fire(new OrderCreated()); return new OrderCreated(); }", "OrderCreated", "create", "(OrderRequest request)", annotation(1, "@POST"))
            )
        );

        var owner = classByQualifiedName(accumulator, "com.example.orders.OrderResource");
        var method = methodByOwner(accumulator, "com.example.orders.OrderResource", "create");

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.fromEntityId().equals(owner.id()) && rel.toEntityId().equals(method.id()) && rel.kind().name().equals("CONTAINS")));
        assertEquals("OrderCreated", method.metadata().get("returnType"));
        assertEquals(Boolean.TRUE, method.metadata().get("jaxRsEndpoint"));
        assertEquals("com.example.orders.OrderCreated", method.metadata().get("cdiPublishedEventType"));
        long apiDependencies = accumulator.relationships().stream()
            .filter(rel -> rel.fromEntityId().equals(owner.id()))
            .filter(rel -> String.valueOf(rel.metadata().getOrDefault("dependencyCategory", "")).equals("api"))
            .count();
        assertTrue(apiDependencies >= 2);
    }
}
