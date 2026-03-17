package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStructuralExtractionContractRegressionTest extends AbstractStructuralExtractionServiceTestSupport {
    @Test
    void usesCompactDisplayNamesForPackagesAndFunctions() {
        String source = """
            package com.example.demo;
            public class DemoController {
                public String hello() { return "hi"; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, source.length(), 1, 0, 3, 1, false, false,
                "public class DemoController { public String hello() { return \"hi\"; } }", List.of(
                    new SyntaxNode("identifier", true, 39, 53, 1, 13, 1, 27, false, false, "DemoController", List.of()),
                    new SyntaxNode("method_declaration", true, 58, 95, 2, 4, 2, 41, false, false,
                        "public String hello() { return \"hi\"; }", List.of(
                            new SyntaxNode("identifier", true, 72, 77, 2, 18, 2, 23, false, false, "hello", List.of()),
                            new SyntaxNode("formal_parameters", true, 77, 79, 2, 23, 2, 25, false, false, "()", List.of())
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoController.java"), "src/main/java/com/example/demo/DemoController.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind().name().equals("PACKAGE")
            && "com.example.demo".equals(scope.name())
            && "demo".equals(scope.displayName())
            && scope.parentScopeId() != null
            && !"scope:repo".equals(scope.parentScopeId())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "hello".equals(entity.name()) && "hello()".equals(entity.displayName())));
    }
    @Test
    void fileScopesUseContainingDirectoryAsParentAndCompactDisplayName() {
        String source = """
            export function canRequestHint() { return true; }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 0, source.length(), false, false, source, List.of(
            new SyntaxNode("function_declaration", true, 0, source.length(), 0, 0, 0, source.length(), false, false, source, List.of(
                new SyntaxNode("identifier", true, 16, 30, 0, 16, 0, 30, false, false, "canRequestHint", List.of())
            ))
        ));

        String relativePath = "src/pages/game/useHintController.ts";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind().name().equals("FILE")
            && relativePath.equals(scope.name())
            && "useHintController.ts".equals(scope.displayName())
            && IdUtils.scopeId("directory", "src/pages/game").equals(scope.parentScopeId())));
    }
    @Test
    void extractsJaxRsResourcesEndpointsAndParameterKindsFromJavaSyntaxTree() {
        String source = """
            package com.example.orders.api;
            @Path("/orders")
            public class OrderResource {
                @GET
                @Path("/{id}")
                public OrderDto getOrder(@PathParam("id") String id, @QueryParam("expand") boolean expand) {
                    return null;
                }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 7, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 31, 0, 0, 0, 31, false, false, "package com.example.orders.api;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 30, 0, 8, 0, 30, false, false, "com.example.orders.api", List.of())
            )),
            new SyntaxNode("class_declaration", true, 32, source.length(), 1, 0, 7, 0, false, false,
                "@Path(\"/orders\") public class OrderResource { @GET @Path(\"/{id}\") public OrderDto getOrder(@PathParam(\"id\") String id, @QueryParam(\"expand\") boolean expand) { return null; } }",
                List.of(
                    new SyntaxNode("annotation", true, 32, 48, 1, 0, 1, 16, false, false, "@Path(\"/orders\")", List.of()),
                    new SyntaxNode("identifier", true, 62, 75, 2, 17, 2, 30, false, false, "OrderResource", List.of()),
                    new SyntaxNode("method_declaration", true, 78, source.length()-2, 3, 4, 6, 5, false, false,
                        "@GET @Path(\"/{id}\") public OrderDto getOrder(@PathParam(\"id\") String id, @QueryParam(\"expand\") boolean expand) { return null; }",
                        List.of(
                            new SyntaxNode("marker_annotation", true, 78, 82, 3, 4, 3, 8, false, false, "@GET", List.of()),
                            new SyntaxNode("annotation", true, 83, 97, 4, 4, 4, 18, false, false, "@Path(\"/{id}\")", List.of()),
                            new SyntaxNode("type_identifier", true, 105, 113, 5, 11, 5, 19, false, false, "OrderDto", List.of()),
                            new SyntaxNode("identifier", true, 114, 122, 5, 20, 5, 28, false, false, "getOrder", List.of()),
                            new SyntaxNode("formal_parameters", true, 122, 192, 5, 28, 5, 98, false, false, "(@PathParam(\"id\") String id, @QueryParam(\"expand\") boolean expand)", List.of())
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/orders/api/OrderResource.java"), "src/main/java/com/example/orders/api/OrderResource.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS
            && "OrderResource".equals(entity.name())
            && Boolean.TRUE.equals(entity.metadata().get("jaxRsResource"))
            && "/orders".equals(entity.metadata().get("jaxRsBasePath"))));

        var endpoint = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.ENDPOINT)
            .findFirst()
            .orElseThrow();
        assertEquals("GET /orders/{id}", endpoint.name());
        assertEquals("GET", endpoint.metadata().get("httpMethod"));
        assertEquals("/orders/{id}", endpoint.metadata().get("path"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> parameterDetails = (List<Map<String, String>>) endpoint.metadata().get("parameterDetails");
        assertEquals(List.of("PATH", "QUERY"), parameterDetails.stream().map(item -> item.get("parameterKind")).toList());

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXPOSES && "GET /orders/{id}".equals(rel.label())));
    }
}
