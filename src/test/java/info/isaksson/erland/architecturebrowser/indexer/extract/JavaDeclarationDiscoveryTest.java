package info.isaksson.erland.architecturebrowser.indexer.extract;

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

class JavaDeclarationDiscoveryTest {

    @Test
    void discoversQualifiedAndSimpleNamesForTopLevelAndNestedTypes() {
        String source = """
            package com.example.orders;

            public class OrderResource {
                static class Mapper {
                }
            }
            """;
        SyntaxNode nestedType = node("class_declaration", "Mapper", 4, 4);
        SyntaxNode topLevelType = node(
            "class_declaration",
            "public class OrderResource { static class Mapper {} }",
            2,
            4,
            node("identifier", "OrderResource", 2, 2),
            nestedType
        );
        SyntaxNode root = node(
            "program",
            source,
            0,
            4,
            node("package_declaration", "package com.example.orders;", 0, 0),
            topLevelType
        );

        Map<String, JavaDeclaredType> discovered = JavaDeclarationDiscovery.discoverDeclaredTypes(
            parseResult("src/main/java/com/example/orders/OrderResource.java", source, root),
            "src/main/java/com/example/orders/OrderResource.java",
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            root
        );

        assertEquals("com.example.orders.OrderResource", discovered.get("OrderResource").qualifiedName());
        assertEquals("com.example.orders.OrderResource", discovered.get("com.example.orders.OrderResource").qualifiedName());
        assertEquals("com.example.orders.OrderResource.Mapper", discovered.get("Mapper").qualifiedName());
        assertEquals("com.example.orders.OrderResource.Mapper", discovered.get("com.example.orders.OrderResource.Mapper").qualifiedName());
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
