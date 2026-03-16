package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaEntityMapperTest {

    private final JavaEntityMapper mapper = new JavaEntityMapper();

    @Test
    void mapsTypeFieldAndMethodEntitiesWithStableCoreMetadata() {
        String source = "package com.example.orders; class OrderResource { private OrderService service; Order findById(String id) { return null; } }";
        SourceParseResult parseResult = parseResult("src/main/java/com/example/orders/OrderResource.java", source);

        SyntaxNode typeNode = node(
            "class_declaration",
            "class OrderResource { private OrderService service; Order findById(String id) { return null; } }",
            1,
            1,
            node("identifier", "OrderResource", 1, 1)
        );
        ExtractedEntityFact typeEntity = mapper.toTypeEntity(
            parseResult,
            parseResult.request().relativePath(),
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            typeNode,
            null
        );
        assertNotNull(typeEntity);
        assertEquals("com.example.orders.OrderResource", typeEntity.metadata().get("qualifiedName"));

        SyntaxNode fieldNode = node("field_declaration", "private OrderService service;", 1, 1);
        List<ExtractedEntityFact> fields = mapper.toFieldEntities(
            parseResult,
            parseResult.request().relativePath(),
            ExtractionMode.SYNTAX_TREE,
            "scope:file:src/main/java/com/example/orders/OrderResource.java",
            fieldNode,
            "com.example.orders.OrderResource"
        );
        assertEquals(1, fields.size());
        assertEquals("OrderService", fields.getFirst().metadata().get("declaredType"));

        SyntaxNode methodNode = node("method_declaration", "Order findById(String id) { return null; }", 1, 1);
        ExtractedEntityFact methodEntity = mapper.toMethodEntity(
            parseResult,
            parseResult.request().relativePath(),
            ExtractionMode.SYNTAX_TREE,
            "scope:file:src/main/java/com/example/orders/OrderResource.java",
            methodNode,
            "com.example.orders.OrderResource"
        );
        assertNotNull(methodEntity);
        assertEquals("Order", methodEntity.metadata().get("returnType"));
        assertEquals(List.of("String"), methodEntity.metadata().get("parameterTypes"));
    }

    private static SourceParseResult parseResult(String relativePath, String source) {
        SyntaxNode root = node("program", source, 0, 1);
        SourceParseRequest request = new SourceParseRequest(null, relativePath, ParseLanguage.JAVA, source);
        SyntaxTree tree = new SyntaxTree(ParseLanguage.JAVA, "test", root, false, root.nodeCount());
        return new SourceParseResult(request, ParseStatus.SUCCESS, tree, List.of(), Map.of());
    }

    private static SyntaxNode node(String type, String text, int startLine, int endLine, SyntaxNode... children) {
        return new SyntaxNode(type, true, 0, text.length(), startLine, 0, endLine, text.length(), false, false, text, List.of(children));
    }
}
