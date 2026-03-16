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

class JavaWritePathStructuralExtractionTest {

    @Test
    void extractsRepositoryAndServiceWritePathsForEntityUpdates() {
        String source = """
            package com.example.orders.service;

            class OrderEntity {}

            class OrderRepository {
                void save(OrderEntity entity) {
                }
            }

            class OrderService {
                OrderRepository orderRepository;

                void createOrder() {
                    OrderEntity entity = new OrderEntity();
                    orderRepository.save(entity);
                }
            }
            """;

        SyntaxNode root = program(source,
            packageDecl(0, "package com.example.orders.service;"),
            classDecl(2, "OrderEntity", "class OrderEntity {}"),
            classDecl(4, "OrderRepository", "class OrderRepository { void save(OrderEntity entity) {} }",
                methodDecl(5, "void save(OrderEntity entity) {}", "save", "(OrderEntity entity)")
            ),
            classDecl(9, "OrderService", "class OrderService { OrderRepository orderRepository; void createOrder() { OrderEntity entity = new OrderEntity(); orderRepository.save(entity); } }",
                fieldDecl(10, "OrderRepository orderRepository;", "orderRepository"),
                methodDecl(12, "void createOrder() { OrderEntity entity = new OrderEntity(); orderRepository.save(entity); }", "createOrder", "()")
            )
        );

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/service/OrderService.java", source, root);

        var entityType = entityByQualifiedName(result, "com.example.orders.service.OrderEntity");
        var createOrder = method(result, "com.example.orders.service.OrderService", "createOrder");
        var repositorySave = method(result, "com.example.orders.service.OrderRepository", "save");

        assertEquals(Boolean.TRUE, createOrder.metadata().get("writePath"));
        assertTrue(String.valueOf(createOrder.metadata().get("writeOperations")).contains("persist"));
        assertTrue(String.valueOf(createOrder.metadata().get("writeEntityTypes")).contains("com.example.orders.service.OrderEntity"));

        assertEquals(Boolean.TRUE, repositorySave.metadata().get("writePath"));
        assertTrue(String.valueOf(repositorySave.metadata().get("writeOperations")).contains("persist"));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(createOrder.id())
            && rel.toEntityId().equals(entityType.id())
            && "writePath".equals(rel.metadata().get("relationshipType"))
            && "persist".equals(rel.metadata().get("writeOperation"))));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(repositorySave.id())
            && rel.toEntityId().equals(entityType.id())
            && "writePath".equals(rel.metadata().get("relationshipType"))
            && "persist".equals(rel.metadata().get("writeOperation"))));
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

    private static info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact entityByQualifiedName(StructuralExtractionResult result, String qualifiedName) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS)
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact method(StructuralExtractionResult result, String ownerQualifiedName, String name) {
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

    private static SyntaxNode packageDecl(int line, String snippet) {
        return new SyntaxNode("package_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode classDecl(int line, String name, String snippet, SyntaxNode... members) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.addAll(List.of(members));
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode fieldDecl(int line, String snippet, String name) {
        return new SyntaxNode("field_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            new SyntaxNode("variable_declarator", true, 0, 0, line, 0, line, 0, false, false, name, List.of(
                new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
            ))
        ));
    }

    private static SyntaxNode methodDecl(int line, String snippet, String name, String parameters) {
        return new SyntaxNode("method_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()),
            new SyntaxNode("formal_parameters", true, 0, 0, line, 0, line, 0, false, false, parameters, List.of())
        ));
    }
}
