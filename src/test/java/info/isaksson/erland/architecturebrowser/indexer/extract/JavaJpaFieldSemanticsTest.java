package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class JavaJpaFieldSemanticsTest {

    @Test
    void appliesJpaAssociationMetadataThroughDedicatedHelper() {
        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderEntity.java",
            "package com.example.orders; @Entity class OrderEntity { @ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer; } @Entity class CustomerEntity {}",
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(
                1,
                "OrderEntity",
                "@Entity class OrderEntity { @ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer; }",
                annotation(1, "@Entity"),
                fieldDecl(
                    1,
                    "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer;",
                    "CustomerEntity",
                    "customer",
                    annotation(1, "@ManyToOne"),
                    annotation(1, "@JoinColumn(name = \"customer_id\", nullable = false)")
                )
            ),
            classDecl(2, "CustomerEntity", "@Entity class CustomerEntity {}", annotation(2, "@Entity"))
        );

        var field = fieldByOwner(accumulator, "com.example.orders.OrderEntity", "customer");
        assertEquals("many-to-one", field.metadata().get("jpaAssociation"));
        assertEquals("customer_id", field.metadata().get("joinColumn"));
        assertEquals(Boolean.FALSE, field.metadata().get("nullable"));
    }
}
