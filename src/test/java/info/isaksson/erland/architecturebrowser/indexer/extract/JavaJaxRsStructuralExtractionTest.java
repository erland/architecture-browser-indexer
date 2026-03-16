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
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaJaxRsStructuralExtractionTest {

    @Test
    void extractsJaxRsResourcesEndpointsAndParameterKinds() {
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
                    new SyntaxNode("method_declaration", true, 78, source.length() - 2, 3, 4, 6, 5, false, false,
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

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/api/OrderResource.java", source, root);

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

    private static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));
    }
}
