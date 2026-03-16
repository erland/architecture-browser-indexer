package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaExtractionSeamHardeningTest {

    @Test
    void declarationMappingAndRelationshipEmissionStayAlignedOnQualifiedNames() {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(java.nio.file.Path.of("/tmp/OrderService.java"), "src/main/java/com/example/orders/OrderService.java", ParseLanguage.JAVA, "class OrderService {}"),
            ParseStatus.SUCCESS,
            new SyntaxTree(
                ParseLanguage.JAVA,
                "test",
                typeNode("program", "program", 0, 3,
                    typeNode("class_declaration", "class OrderService extends BaseService implements Auditable { }", 1, 3,
                        typeNode("identifier", "OrderService", 1, 1)
                    )
                ),
                false,
                3
            ),
            List.<ParseIssue>of(),
            Map.of()
        );
        SyntaxNode typeNode = parseResult.syntaxTree().root().children().getFirst();

        Map<String, JavaDeclaredType> declaredTypes = JavaDeclarationDiscovery.discoverDeclaredTypes(
            parseResult,
            "src/main/java/com/example/orders/OrderService.java",
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            parseResult.syntaxTree().root()
        );

        JavaEntityMapper entityMapper = new JavaEntityMapper();
        ExtractedEntityFact typeEntity = entityMapper.toTypeEntity(
            parseResult,
            "src/main/java/com/example/orders/OrderService.java",
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            typeNode,
            null
        );

        assertNotNull(typeEntity);
        assertEquals("com.example.orders.OrderService", typeEntity.metadata().get("qualifiedName"));
        assertEquals("com.example.orders.OrderService", declaredTypes.get("OrderService").qualifiedName());
        assertEquals("com.example.orders.OrderService", declaredTypes.get("com.example.orders.OrderService").qualifiedName());

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        accumulator.addEntity(typeEntity);

        new JavaRelationshipEvidenceEmitter().addTypeRelationships(
            accumulator,
            "src/main/java/com/example/orders/OrderService.java",
            "com.example.orders",
            typeNode,
            typeEntity,
            Map.of(),
            declaredTypes
        );

        assertTrue(accumulator.relationships().stream().anyMatch(relationship ->
            relationship.kind() == RelationshipKind.EXTENDS
                && "extends".equals(relationship.metadata().get("dependencySource"))
                && "hierarchy".equals(relationship.metadata().get("dependencyCategory"))
        ));
        assertTrue(accumulator.relationships().stream().anyMatch(relationship ->
            relationship.kind() == RelationshipKind.IMPLEMENTS
                && "implements".equals(relationship.metadata().get("dependencySource"))
                && "hierarchy".equals(relationship.metadata().get("dependencyCategory"))
        ));
        assertTrue(accumulator.relationships().stream().anyMatch(relationship ->
            relationship.kind() == RelationshipKind.DEPENDS_ON
                && "hierarchy".equals(relationship.metadata().get("dependencyCategory"))
        ));
    }

    @Test
    void semanticHelperContractsStayStableForJpaCdiAndWritePaths() {
        JavaJpaDetailSupport jpaSupport = new JavaJpaDetailSupport();
        JavaCdiDetailSupport cdiSupport = new JavaCdiDetailSupport();
        JavaWritePathDetailSupport writePathSupport = new JavaWritePathDetailSupport();

        ExtractedEntityFact field = new ExtractedEntityFact(
            "field:customer",
            EntityKind.FIELD,
            EntityOrigin.OBSERVED,
            "customer",
            "customer",
            "scope:file",
            List.of(new SourceReference(
                "src/main/java/com/example/orders/Order.java",
                10,
                10,
                "@ManyToOne @JoinColumn(name = \"customer_id\") private Customer customer;",
                Map.of()
            )),
            Map.of(
                "annotations", List.of("ManyToOne"),
                "declaredType", "Customer"
            )
        );
        ExtractedEntityFact observerMethod = new ExtractedEntityFact(
            "method:onOrderCreated",
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            "onOrderCreated",
            "onOrderCreated",
            "scope:file",
            List.of(),
            Map.of("parameters", "(@ObservesAsync @Critical OrderCreated event)")
        );
        ExtractedEntityFact writeMethod = new ExtractedEntityFact(
            "method:createOrder",
            EntityKind.FUNCTION,
            EntityOrigin.OBSERVED,
            "createOrder",
            "createOrder",
            "scope:file",
            List.of(),
            Map.of(
                "parameters", "(Order order)",
                "parameterTypes", List.of("Order")
            )
        );

        var association = jpaSupport.analyzeField(field, field.sourceRefs().getFirst().snippet());
        Optional<JavaCdiDetailSupport.ObservedEvent> observedEvent = cdiSupport.detectObservedEvent(observerMethod, "");
        var writePaths = writePathSupport.detectWritePaths(
            writeMethod,
            "Order saved = orderRepository.save(order); entityManager.merge(saved);"
        );

        assertEquals("many-to-one", association.associationKind());
        assertEquals("customer_id", association.joinColumn());
        assertTrue(cdiSupport.detectPublishedEvents("events.fire(new OrderCreated(id));", "class Publisher { @Inject Event<OrderCreated> events; }")
            .stream().anyMatch(event -> event.publisherField().equals("events")));
        assertTrue(observedEvent.isPresent());
        assertEquals("OrderCreated", observedEvent.orElseThrow().eventType());
        assertTrue(observedEvent.orElseThrow().async());
        assertEquals(2, writePaths.size());
    }

    private static SyntaxNode typeNode(String type, String text, int startLine, int endLine, SyntaxNode... children) {
        return new SyntaxNode(type, true, 0, text.length(), startLine, 0, endLine, text.length(), false, false, text, List.of(children));
    }
}
