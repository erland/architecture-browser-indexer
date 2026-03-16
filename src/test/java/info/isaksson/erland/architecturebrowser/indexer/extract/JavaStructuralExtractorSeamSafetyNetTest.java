package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStructuralExtractorSeamSafetyNetTest {

    @Test
    void preservesDeclarationContainmentAndMemberMetadataAcrossFutureSplits() {
        String source = """
            package com.example.orders;
            class OrderService {
                @Inject
                private final OrderRepository repository;

                public Order handle(OrderRequest request, Instant at) {
                    return null;
                }
            }
            """;

        SyntaxNode root = program(source,
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            classDecl(1, "OrderService", "class OrderService { ... }",
                fieldDecl(3, "@Inject private final OrderRepository repository;", "OrderRepository", "repository", annotation(2, "@Inject")),
                methodDecl(5, "public Order handle(OrderRequest request, Instant at) { return null; }", "Order", "handle", "(OrderRequest request, Instant at)")
            )
        );

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/OrderService.java", source, root);

        ExtractedEntityFact owner = classByQualifiedName(result, "com.example.orders.OrderService");
        ExtractedEntityFact repository = field(result, "com.example.orders.OrderService", "repository");
        ExtractedEntityFact handle = method(result, "com.example.orders.OrderService", "handle");

        assertEquals("OrderService", owner.displayName());
        assertEquals("OrderRepository", repository.metadata().get("declaredType"));
        assertEquals(List.of("Inject"), repository.metadata().get("annotations"));
        assertEquals(List.of("private", "final"), repository.metadata().get("modifiers"));
        assertEquals("Order", handle.metadata().get("returnType"));
        assertEquals(List.of("OrderRequest", "Instant"), handle.metadata().get("parameterTypes"));
        assertEquals("com.example.orders.OrderService", handle.metadata().get("ownerQualifiedName"));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && owner.id().equals(rel.fromEntityId())
            && repository.id().equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && owner.id().equals(rel.fromEntityId())
            && handle.id().equals(rel.toEntityId())));
    }

    @Test
    void preservesHierarchyAndDeclaredTypeDependencyMetadataAcrossFutureSplits() {
        String source = """
            package com.example.orders;
            import java.util.List;
            import com.example.shared.RequestContext;
            interface OrdersPort {}
            class BaseService {}
            class OrderService extends BaseService implements OrdersPort {
                private List<RequestContext> contexts;
                public RequestContext load(RequestContext request) {
                    return request;
                }
            }
            """;

        SyntaxNode root = program(source,
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            importDecl(1, "import java.util.List;"),
            importDecl(2, "import com.example.shared.RequestContext;"),
            interfaceDecl(3, "OrdersPort", "interface OrdersPort {}"),
            classDecl(4, "BaseService", "class BaseService {}"),
            classDecl(5, "OrderService", "class OrderService extends BaseService implements OrdersPort { ... }",
                typeIdentifier(5, "BaseService"),
                typeIdentifier(5, "OrdersPort"),
                fieldDecl(6, "private List<RequestContext> contexts;", "List<RequestContext>", "contexts"),
                methodDecl(7, "public RequestContext load(RequestContext request) { return request; }", "RequestContext", "load", "(RequestContext request)")
            )
        );

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/OrderService.java", source, root);
        ExtractedEntityFact orderService = classByQualifiedName(result, "com.example.orders.OrderService");

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && orderService.id().equals(rel.fromEntityId())
            && "com.example.orders.BaseService".equals(rel.label())
            && "extends".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && orderService.id().equals(rel.fromEntityId())
            && "com.example.orders.OrdersPort".equals(rel.label())
            && "implements".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && orderService.id().equals(rel.fromEntityId())
            && "java.util.List".equals(rel.label())
            && "field".equals(rel.metadata().get("dependencySource"))
            && "composition".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && orderService.id().equals(rel.fromEntityId())
            && "com.example.shared.RequestContext".equals(rel.label())
            && "parameterType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && orderService.id().equals(rel.fromEntityId())
            && "com.example.shared.RequestContext".equals(rel.label())
            && "returnType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
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

    private static ExtractedEntityFact classByQualifiedName(StructuralExtractionResult result, String qualifiedName) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS)
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ExtractedEntityFact field(StructuralExtractionResult result, String ownerQualifiedName, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FIELD)
            .filter(entity -> name.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ExtractedEntityFact method(StructuralExtractionResult result, String ownerQualifiedName, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION)
            .filter(entity -> name.equals(entity.name()))
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

    private static SyntaxNode methodDecl(int line, String snippet, String returnType, String name, String parameters) {
        return new SyntaxNode("method_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            typeIdentifier(line, returnType),
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()),
            new SyntaxNode("formal_parameters", true, 0, 0, line, 0, line, 0, false, false, parameters, List.of())
        ));
    }

    private static SyntaxNode annotation(int line, String snippet) {
        return new SyntaxNode("annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
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
