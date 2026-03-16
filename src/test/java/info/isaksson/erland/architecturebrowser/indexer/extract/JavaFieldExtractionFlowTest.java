package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaFieldExtractionFlowTest {

    @Test
    void extractsFieldEntitiesContainmentDependenciesAndJpaMetadata() {
        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderEntity.java",
            "package com.example.orders; @Entity class OrderEntity { @ManyToOne CustomerEntity customer; }",
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(
                1,
                "OrderEntity",
                "@Entity class OrderEntity { @ManyToOne CustomerEntity customer; }",
                annotation(1, "@Entity"),
                fieldDecl(1, "@ManyToOne CustomerEntity customer;", "CustomerEntity", "customer", annotation(1, "@ManyToOne"))
            ),
            classDecl(2, "CustomerEntity", "@Entity class CustomerEntity {}", annotation(2, "@Entity"))
        );

        var owner = classByQualifiedName(accumulator, "com.example.orders.OrderEntity");
        var field = fieldByOwner(accumulator, "com.example.orders.OrderEntity", "customer");

        assertEquals("CustomerEntity", field.metadata().get("declaredType"));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.fromEntityId().equals(owner.id()) && rel.toEntityId().equals(field.id()) && rel.kind().name().equals("CONTAINS")));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.fromEntityId().equals(owner.id()) && String.valueOf(rel.metadata().get("dependencySource")).equals("field")));
        assertEquals("many-to-one", field.metadata().get("jpaAssociation"));
        assertEquals("jpa", field.metadata().get("framework"));
    }
}
