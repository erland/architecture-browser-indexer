package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class JavaJpaDomainSemanticsSupportsSeamTest {

    @Test
    void preservesJpaTypePropertyAndAssociationContractsThroughDedicatedSupports() {
        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderEntity.java",
            "package com.example.orders; @Entity @Table(name = \"orders\") class OrderEntity extends BaseEntity { @ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer; @Embedded AddressValue shippingAddress; @Column(name = \"external_id\") public String getExternalId() { return \"x\"; } } @Entity class CustomerEntity {} @Embeddable class AddressValue {} @MappedSuperclass class BaseEntity {}",
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(
                1,
                "OrderEntity",
                "@Entity @Table(name = \"orders\") class OrderEntity extends BaseEntity { @ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer; @Embedded AddressValue shippingAddress; @Column(name = \"external_id\") public String getExternalId() { return \"x\"; } }",
                annotation(1, "@Entity"),
                annotation(1, "@Table(name = \"orders\")"),
                fieldDecl(1, "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer;", "CustomerEntity", "customer", annotation(1, "@ManyToOne"), annotation(1, "@JoinColumn(name = \"customer_id\", nullable = false)")),
                fieldDecl(1, "@Embedded AddressValue shippingAddress;", "AddressValue", "shippingAddress", annotation(1, "@Embedded")),
                methodDecl(1, "@Column(name = \"external_id\") public String getExternalId() { return \"x\"; }", "String", "getExternalId", "()", annotation(1, "@Column(name = \"external_id\")"))
            ),
            classDecl(2, "CustomerEntity", "@Entity class CustomerEntity {}", annotation(2, "@Entity")),
            classDecl(3, "AddressValue", "@Embeddable class AddressValue {}", annotation(3, "@Embeddable")),
            classDecl(4, "BaseEntity", "@MappedSuperclass class BaseEntity {}", annotation(4, "@MappedSuperclass"))
        );

        var customerField = fieldByOwner(accumulator, "com.example.orders.OrderEntity", "customer");
        assertEquals("many-to-one", customerField.metadata().get("jpaAssociation"));
        assertEquals("customer_id", customerField.metadata().get("joinColumn"));

        var embeddedField = fieldByOwner(accumulator, "com.example.orders.OrderEntity", "shippingAddress");
        assertEquals(true, embeddedField.metadata().get("jpaEmbedded"));

        var getter = methodByOwner(accumulator, "com.example.orders.OrderEntity", "getExternalId");
        assertEquals(true, getter.metadata().get("jpaPropertyAccess"));
        assertEquals("externalId", getter.metadata().get("jpaPropertyName"));
        assertEquals("external_id", getter.metadata().get("columnName"));

        var orderEntity = accumulator.entities().stream()
            .filter(entity -> "com.example.orders.OrderEntity".equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
        assertEquals("entity", orderEntity.metadata().get("jpaKind"));
        assertEquals("orders", orderEntity.metadata().get("tableName"));

        assertNotNull(accumulator.relationships().stream()
            .filter(relationship -> orderEntity.id().equals(relationship.fromEntityId()))
            .filter(relationship -> "hasAssociation".equals(relationship.metadata().get("relationshipType")))
            .filter(relationship -> "jpa".equals(relationship.metadata().get("framework")))
            .filter(relationship -> "0".equals(relationship.metadata().get("sourceLowerBound")))
            .filter(relationship -> "*".equals(relationship.metadata().get("sourceUpperBound")))
            .filter(relationship -> "1".equals(relationship.metadata().get("targetLowerBound")))
            .filter(relationship -> "1".equals(relationship.metadata().get("targetUpperBound")))
            .findFirst()
            .orElse(null));
    }
}
