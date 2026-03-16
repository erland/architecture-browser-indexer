package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.interfaceDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.typeIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStageTypeFlowSafetyNetTest {

    @Test
    void freezesTypeDiscoveryOwnershipAndHierarchyEmission() {
        String source = """
            package com.example.orders;

            class BaseService {}
            interface OrdersPort {}
            class OrderService extends BaseService implements OrdersPort {
                class NestedValidator {}
            }
            """;

        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderService.java",
            source,
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(2, "BaseService", "class BaseService {}"),
            interfaceDecl(3, "OrdersPort", "interface OrdersPort {}"),
            classDecl(4, "OrderService", "class OrderService extends BaseService implements OrdersPort { ... }",
                typeIdentifier(4, "BaseService"),
                typeIdentifier(4, "OrdersPort"),
                classDecl(5, "NestedValidator", "class NestedValidator {}")
            )
        );

        ExtractedEntityFact orderService = classByQualifiedName(accumulator, "com.example.orders.OrderService");
        ExtractedEntityFact nestedValidator = classByQualifiedName(accumulator, "com.example.orders.OrderService.NestedValidator");

        assertEquals(1, accumulator.filesVisited());
        assertEquals(1, accumulator.filesExtracted());
        assertEquals("com.example.orders.OrderService.NestedValidator", nestedValidator.metadata().get("qualifiedName"));

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && orderService.id().equals(rel.fromEntityId())
            && "extends".equals(rel.metadata().get("dependencySource"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && orderService.id().equals(rel.fromEntityId())
            && "implements".equals(rel.metadata().get("dependencySource"))));
        assertEquals("NestedValidator", nestedValidator.name());
    }
}
