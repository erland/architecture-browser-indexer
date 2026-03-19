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

class JavaJpaEntityNamingRegressionTest {

    private final JavaEntityMapper mapper = new JavaEntityMapper();

    @Test
    void usesDeclaredJavaClassNameInsteadOfAnnotationNameForJpaEntity() {
        String source = "package com.example.orders; @Entity public class OrderEntity { private String id; }";
        SourceParseResult parseResult = parseResult("src/main/java/com/example/orders/OrderEntity.java", source);

        SyntaxNode typeNode = node(
            "class_declaration",
            "@Entity public class OrderEntity { private String id; }",
            1,
            1,
            node("marker_annotation", "@Entity", 1, 1,
                node("identifier", "Entity", 1, 1)
            ),
            node("modifiers", "public", 1, 1),
            node("identifier", "OrderEntity", 1, 1),
            node("class_body", "{ private String id; }", 1, 1)
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
        assertEquals("OrderEntity", typeEntity.name());
        assertEquals("com.example.orders.OrderEntity", typeEntity.metadata().get("qualifiedName"));
    }

    @Test
    void declarationDiscoveryUsesDeclaredJavaClassNameInsteadOfAnnotationNameForJpaEntity() {
        String source = "package com.example.orders; @Entity public class OrderEntity { }";
        SyntaxNode typeNode = node(
            "class_declaration",
            "@Entity public class OrderEntity { }",
            1,
            1,
            node("marker_annotation", "@Entity", 1, 1,
                node("identifier", "Entity", 1, 1)
            ),
            node("modifiers", "public", 1, 1),
            node("identifier", "OrderEntity", 1, 1),
            node("class_body", "{ }", 1, 1)
        );
        SyntaxNode root = node("program", source, 0, 1,
            node("package_declaration", "package com.example.orders;", 0, 0),
            typeNode
        );

        Map<String, JavaDeclaredType> discovered = JavaDeclarationDiscovery.discoverDeclaredTypes(
            parseResult("src/main/java/com/example/orders/OrderEntity.java", source, root),
            "src/main/java/com/example/orders/OrderEntity.java",
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            root
        );

        assertEquals("com.example.orders.OrderEntity", discovered.get("OrderEntity").qualifiedName());
        assertEquals("com.example.orders.OrderEntity", discovered.get("com.example.orders.OrderEntity").qualifiedName());
    }

    private static SourceParseResult parseResult(String relativePath, String source) {
        SyntaxNode root = node("program", source, 0, 1);
        SourceParseRequest request = new SourceParseRequest(null, relativePath, ParseLanguage.JAVA, source);
        SyntaxTree tree = new SyntaxTree(ParseLanguage.JAVA, "test", root, false, root.nodeCount());
        return new SourceParseResult(request, ParseStatus.SUCCESS, tree, List.of(), Map.of());
    }

    private static SourceParseResult parseResult(String relativePath, String source, SyntaxNode root) {
        SourceParseRequest request = new SourceParseRequest(null, relativePath, ParseLanguage.JAVA, source);
        SyntaxTree tree = new SyntaxTree(ParseLanguage.JAVA, "test", root, false, root.nodeCount());
        return new SourceParseResult(request, ParseStatus.SUCCESS, tree, List.of(), Map.of());
    }

    private static SyntaxNode node(String type, String text, int startLine, int endLine, SyntaxNode... children) {
        return new SyntaxNode(type, true, 0, text.length(), startLine, 0, endLine, text.length(), false, false, text, List.of(children));
    }
}
