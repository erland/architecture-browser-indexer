package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.markerAnnotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStageSemanticEnrichmentSafetyNetTest {

    @Test
    void freezesJaxRsJpaCdiAndWritePathSemantics() {
        String source = """
            package com.example.orders;
            @Path("/orders")
            class OrderResource {
                @GET
                Order find(OrderRequest request) { return null; }
            }
            @Entity
            class OrderEntity {
                @ManyToOne
                @JoinColumn(name = "customer_id", nullable = false)
                CustomerEntity customer;
            }
            @Entity
            class CustomerEntity {}
            class OrderEvents {
                void onCreated(@Observes OrderCreated event) {}
            }
            class OrderService {
                void save(OrderEntity entity) { entityManager.persist(entity); }
            }
            """;

        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderResource.java",
            source,
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(1, "OrderResource", "@Path(\"/orders\") class OrderResource { ... }",
                annotation(1, "@Path(\"/orders\")"),
                methodDecl(3, "@GET Order find(OrderRequest request) { return null; }", "Order", "find", "(OrderRequest request)", markerAnnotation(2, "@GET"))
            ),
            classDecl(6, "OrderEntity", "@Entity class OrderEntity { ... }",
                annotation(6, "@Entity"),
                fieldDecl(7, "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer;", "CustomerEntity", "customer",
                    annotation(7, "@ManyToOne"),
                    annotation(7, "@JoinColumn(name = \"customer_id\", nullable = false)")
                )
            ),
            classDecl(10, "CustomerEntity", "@Entity class CustomerEntity {}",
                annotation(10, "@Entity")
            ),
            classDecl(11, "OrderEvents", "class OrderEvents { ... }",
                methodDecl(12, "void onCreated(@Observes OrderCreated event) {}", "void", "onCreated", "(@Observes OrderCreated event)")
            ),
            classDecl(14, "OrderService", "class OrderService { ... }",
                methodDecl(15, "void save(OrderEntity entity) { entityManager.persist(entity); }", "void", "save", "(OrderEntity entity)")
            )
        );

        ExtractedEntityFact resource = classByQualifiedName(accumulator, "com.example.orders.OrderResource");
        ExtractedEntityFact customerField = fieldByOwner(accumulator, "com.example.orders.OrderEntity", "customer");
        ExtractedEntityFact observerMethod = methodByOwner(accumulator, "com.example.orders.OrderEvents", "onCreated");
        ExtractedEntityFact writeMethod = methodByOwner(accumulator, "com.example.orders.OrderService", "save");
        ExtractedEntityFact endpoint = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.ENDPOINT)
            .findFirst()
            .orElseThrow();

        assertEquals(Boolean.TRUE, resource.metadata().get("jaxRsResource"));
        assertEquals("/orders", resource.metadata().get("jaxRsBasePath"));
        assertEquals("GET", endpoint.metadata().get("httpMethod"));
        assertEquals("/orders", endpoint.metadata().get("path"));
        assertEquals("many-to-one", customerField.metadata().get("jpaAssociation"));
        assertEquals("customer_id", customerField.metadata().get("joinColumn"));
        assertEquals(Boolean.FALSE, customerField.metadata().get("nullable"));
        assertEquals(Boolean.TRUE, observerMethod.metadata().get("cdiObserver"));
        assertEquals("com.example.orders.OrderCreated", observerMethod.metadata().get("cdiObservedEventType"));
        assertEquals(Boolean.TRUE, writeMethod.metadata().get("writePath"));

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXPOSES
            && resource.id().equals(rel.fromEntityId())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && "jpa".equals(rel.metadata().get("framework"))
            && "association".equals(rel.metadata().get("associationKind"))
            && "many-to-one".equals(rel.metadata().get("associationCardinality"))
            && "many-to-one".equals(rel.metadata().get("jpaAssociation"))
            && "0".equals(rel.metadata().get("sourceLowerBound"))
            && "*".equals(rel.metadata().get("sourceUpperBound"))
            && "1".equals(rel.metadata().get("targetLowerBound"))
            && "1".equals(rel.metadata().get("targetUpperBound"))));
    }
}
