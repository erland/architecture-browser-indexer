package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSyntaxTreeExtractionStageSafetyNetTest {

    @Test
    void preservesStageLevelTypeMemberAndDependencyExtraction() {
        String source = """
            package com.example.orders;
            import java.util.List;
            import com.example.shared.RequestContext;

            class BaseService {}
            interface OrdersPort {}

            class OrderService extends BaseService implements OrdersPort {
                private List<RequestContext> contexts;

                public RequestContext load(RequestContext request) {
                    return request;
                }
            }
            """;

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/orders/OrderService.java"), "src/main/java/com/example/orders/OrderService.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source,
                packageDecl(0, "package com.example.orders;", "com.example.orders"),
                importDecl(1, "import java.util.List;"),
                importDecl(2, "import com.example.shared.RequestContext;"),
                classDecl(4, "BaseService", "class BaseService {}"),
                interfaceDecl(5, "OrdersPort", "interface OrdersPort {}"),
                classDecl(7, "OrderService", "class OrderService extends BaseService implements OrdersPort { ... }",
                    typeIdentifier(7, "BaseService"),
                    typeIdentifier(7, "OrdersPort"),
                    fieldDecl(8, "private List<RequestContext> contexts;", "List<RequestContext>", "contexts"),
                    methodDecl(10, "public RequestContext load(RequestContext request) { return request; }", "RequestContext", "load", "(RequestContext request)")
                )
            ), false, 20),
            List.<ParseIssue>of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        ExtractionAccumulator accumulator = new JavaSyntaxTreeExtractionStage().extract(parseResult, new ExtractionAccumulator());

        ExtractedEntityFact orderService = classByQualifiedName(accumulator, "com.example.orders.OrderService");
        ExtractedEntityFact contexts = fieldByOwner(accumulator, "com.example.orders.OrderService", "contexts");
        ExtractedEntityFact load = methodByOwner(accumulator, "com.example.orders.OrderService", "load");

        assertEquals(1, accumulator.filesVisited());
        assertEquals(1, accumulator.filesExtracted());
        assertEquals("List<RequestContext>", contexts.metadata().get("declaredType"));
        assertEquals(List.of("RequestContext"), load.metadata().get("parameterTypes"));
        assertEquals("RequestContext", load.metadata().get("returnType"));
        assertEquals("com.example.orders.OrderService", load.metadata().get("ownerQualifiedName"));

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && orderService.id().equals(rel.fromEntityId())
            && contexts.id().equals(rel.toEntityId())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && orderService.id().equals(rel.fromEntityId())
            && "extends".equals(rel.metadata().get("dependencySource"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && orderService.id().equals(rel.fromEntityId())
            && "implements".equals(rel.metadata().get("dependencySource"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && orderService.id().equals(rel.fromEntityId())
            && "parameterType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
    }

    @Test
    void preservesStageLevelJaxRsAndJpaSemanticMetadata() {
        String source = """
            package com.example.orders;
            @Path("/orders")
            class OrderResource {
                @GET
                Order find(OrderRequest request) { return null; }
            }
            @Entity
            class OrderEntity {
                @ManyToOne
                @JoinColumn(name = "customer_id", nullable = false)
                CustomerEntity customer;
            }
            @Entity
            class CustomerEntity {}
            """;

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/orders/OrderResource.java"), "src/main/java/com/example/orders/OrderResource.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source,
                packageDecl(0, "package com.example.orders;", "com.example.orders"),
                classDecl(1, "OrderResource", "@Path(\"/orders\") class OrderResource { ... }",
                    annotation(1, "@Path(\"/orders\")"),
                    methodDecl(3, "@GET Order find(OrderRequest request) { return null; }", "Order", "find", "(OrderRequest request)", markerAnnotation(2, "@GET"))
                ),
                classDecl(6, "OrderEntity", "@Entity class OrderEntity { ... }",
                    annotation(6, "@Entity"),
                    fieldDecl(7, "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer;", "CustomerEntity", "customer",
                        annotation(7, "@ManyToOne"),
                        annotation(7, "@JoinColumn(name = \"customer_id\", nullable = false)")
                    )
                ),
                classDecl(10, "CustomerEntity", "@Entity class CustomerEntity {}",
                    annotation(10, "@Entity")
                )
            ), false, 18),
            List.<ParseIssue>of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        ExtractionAccumulator accumulator = new JavaSyntaxTreeExtractionStage().extract(parseResult, new ExtractionAccumulator());

        ExtractedEntityFact resource = classByQualifiedName(accumulator, "com.example.orders.OrderResource");
        ExtractedEntityFact customerField = fieldByOwner(accumulator, "com.example.orders.OrderEntity", "customer");
        ExtractedEntityFact endpoint = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.ENDPOINT)
            .findFirst()
            .orElseThrow();

        assertEquals(Boolean.TRUE, resource.metadata().get("jaxRsResource"));
        assertEquals("/orders", resource.metadata().get("jaxRsBasePath"));
        assertEquals("GET", endpoint.metadata().get("httpMethod"));
        assertEquals("/orders", endpoint.metadata().get("path"));
        assertEquals("many-to-one", customerField.metadata().get("jpaAssociation"));
        assertEquals("customer_id", customerField.metadata().get("joinColumn"));
        assertEquals(Boolean.FALSE, customerField.metadata().get("nullable"));

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXPOSES
            && resource.id().equals(rel.fromEntityId())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && "hasAssociation".equals(rel.metadata().get("relationshipType"))
            && "many-to-one".equals(rel.metadata().get("jpaAssociation"))));
    }

    private static ExtractedEntityFact classByQualifiedName(ExtractionAccumulator accumulator, String qualifiedName) {
        return accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS)
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ExtractedEntityFact fieldByOwner(ExtractionAccumulator accumulator, String ownerQualifiedName, String fieldName) {
        return accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FIELD)
            .filter(entity -> fieldName.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ExtractedEntityFact methodByOwner(ExtractionAccumulator accumulator, String ownerQualifiedName, String methodName) {
        return accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION)
            .filter(entity -> methodName.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, 0, 0, false, false, source, List.of(children));
    }

    private static SyntaxNode packageDecl(int line, String snippet, String qualifiedName) {
        return new SyntaxNode("package_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            new SyntaxNode("scoped_identifier", true, 0, 0, line, 0, line, 0, false, false, qualifiedName, List.of())
        ));
    }

    private static SyntaxNode importDecl(int line, String snippet) {
        return new SyntaxNode("import_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode interfaceDecl(int line, String name, String snippet) {
        return new SyntaxNode("interface_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
        ));
    }

    private static SyntaxNode classDecl(int line, String name, String snippet, SyntaxNode... membersAndTypes) {
        ArrayList<SyntaxNode> children = new ArrayList<>();
        children.add(new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.addAll(List.of(membersAndTypes));
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode fieldDecl(int line, String snippet, String declaredTypeSnippet, String name, SyntaxNode... annotations) {
        ArrayList<SyntaxNode> children = new ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(declaredTypeNode(line, declaredTypeSnippet));
        children.add(new SyntaxNode("variable_declarator", true, 0, 0, line, 0, line, 0, false, false, name, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
        )));
        return new SyntaxNode("field_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode methodDecl(int line, String snippet, String returnType, String name, String parameters, SyntaxNode... annotations) {
        ArrayList<SyntaxNode> children = new ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(typeIdentifier(line, returnType));
        children.add(new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.add(new SyntaxNode("formal_parameters", true, 0, 0, line, 0, line, 0, false, false, parameters, List.of()));
        return new SyntaxNode("method_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode annotation(int line, String snippet) {
        return new SyntaxNode("annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode markerAnnotation(int line, String snippet) {
        return new SyntaxNode("marker_annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode typeIdentifier(int line, String snippet) {
        return new SyntaxNode("type_identifier", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode declaredTypeNode(int line, String snippet) {
        if (!snippet.contains("<")) {
            return typeIdentifier(line, snippet);
        }
        String rawType = snippet.substring(0, snippet.indexOf('<'));
        String genericArgument = snippet.substring(snippet.indexOf('<') + 1, snippet.lastIndexOf('>'));
        return new SyntaxNode("generic_type", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            typeIdentifier(line, rawType),
            typeIdentifier(line, genericArgument)
        ));
    }
}
