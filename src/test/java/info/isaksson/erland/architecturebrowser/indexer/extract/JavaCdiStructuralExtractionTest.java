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

class JavaCdiStructuralExtractionTest {

    @Test
    void extractsCdiEventPublishersObserversAndAsyncObservers() {
        String source = """
            package com.example.orders.events;

            import jakarta.enterprise.event.Event;
            import jakarta.enterprise.event.Observes;
            import jakarta.enterprise.event.ObservesAsync;
            import jakarta.inject.Inject;

            class OrderCreatedEvent {}

            class OrderService {
                @Inject
                Event<OrderCreatedEvent> orderCreatedEvents;

                void createOrder() {
                    orderCreatedEvents.fire(new OrderCreatedEvent());
                }
            }

            class OrderCreatedObserver {
                void onOrderCreated(@Observes OrderCreatedEvent event) {
                }

                void onOrderCreatedAsync(@ObservesAsync OrderCreatedEvent event) {
                }
            }
            """;

        SyntaxNode root = program(source,
            packageDecl(0, "package com.example.orders.events;"),
            importDecl(1, "import jakarta.enterprise.event.Event;"),
            importDecl(2, "import jakarta.enterprise.event.Observes;"),
            importDecl(3, "import jakarta.enterprise.event.ObservesAsync;"),
            importDecl(4, "import jakarta.inject.Inject;"),
            classDecl(6, "OrderCreatedEvent", "class OrderCreatedEvent {}"),
            classDecl(8, "OrderService", """
                class OrderService {
                    @Inject
                    Event<OrderCreatedEvent> orderCreatedEvents;

                    void createOrder() {
                        orderCreatedEvents.fire(new OrderCreatedEvent());
                    }
                }
                """,
                fieldDecl(10, "@Inject Event<OrderCreatedEvent> orderCreatedEvents;", "orderCreatedEvents", annotation(10, "@Inject")),
                methodDecl(13, "void createOrder() { orderCreatedEvents.fire(new OrderCreatedEvent()); }", "createOrder", "()")
            ),
            classDecl(18, "OrderCreatedObserver", """
                class OrderCreatedObserver {
                    void onOrderCreated(@Observes OrderCreatedEvent event) {
                    }

                    void onOrderCreatedAsync(@ObservesAsync OrderCreatedEvent event) {
                    }
                }
                """,
                methodDecl(19, "void onOrderCreated(@Observes OrderCreatedEvent event) {}", "onOrderCreated", "(@Observes OrderCreatedEvent event)", annotation(19, "@Observes")),
                methodDecl(22, "void onOrderCreatedAsync(@ObservesAsync OrderCreatedEvent event) {}", "onOrderCreatedAsync", "(@ObservesAsync OrderCreatedEvent event)", annotation(22, "@ObservesAsync"))
            )
        );

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/events/OrderService.java", source, root);

        var service = entityByQualifiedName(result, "com.example.orders.events.OrderService");
        var eventType = entityByQualifiedName(result, "com.example.orders.events.OrderCreatedEvent");
        var createOrder = method(result, "com.example.orders.events.OrderService", "createOrder");
        var observer = method(result, "com.example.orders.events.OrderCreatedObserver", "onOrderCreated");
        var asyncObserver = method(result, "com.example.orders.events.OrderCreatedObserver", "onOrderCreatedAsync");

        assertEquals(Boolean.TRUE, createOrder.metadata().get("cdiEventPublisher"));
        assertEquals("com.example.orders.events.OrderCreatedEvent", createOrder.metadata().get("cdiPublishedEventType"));
        assertEquals(Boolean.TRUE, observer.metadata().get("cdiObserver"));
        assertEquals(Boolean.FALSE, observer.metadata().get("observerAsync"));
        assertEquals(Boolean.TRUE, asyncObserver.metadata().get("cdiObserver"));
        assertEquals(Boolean.TRUE, asyncObserver.metadata().get("observerAsync"));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(service.id())
            && rel.toEntityId().equals(eventType.id())
            && "publishesEvent".equals(rel.metadata().get("relationshipType"))));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(createOrder.id())
            && rel.toEntityId().equals(eventType.id())
            && "publishesEvent".equals(rel.metadata().get("relationshipType"))
            && "method".equals(rel.metadata().get("ownerMemberKind"))));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(observer.id())
            && rel.toEntityId().equals(eventType.id())
            && "observesEvent".equals(rel.metadata().get("relationshipType"))
            && Boolean.FALSE.equals(rel.metadata().get("observerAsync"))));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(asyncObserver.id())
            && rel.toEntityId().equals(eventType.id())
            && "observesEvent".equals(rel.metadata().get("relationshipType"))
            && Boolean.TRUE.equals(rel.metadata().get("observerAsync"))));
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

    private static SyntaxNode importDecl(int line, String snippet) {
        return new SyntaxNode("import_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode classDecl(int line, String name, String snippet, SyntaxNode... members) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.addAll(List.of(members));
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode fieldDecl(int line, String snippet, String name, SyntaxNode... annotations) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(new SyntaxNode("variable_declarator", true, 0, 0, line, 0, line, 0, false, false, name, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
        )));
        return new SyntaxNode("field_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode methodDecl(int line, String snippet, String name, String parameters, SyntaxNode... annotations) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.add(new SyntaxNode("formal_parameters", true, 0, 0, line, 0, line, 0, false, false, parameters, List.of()));
        return new SyntaxNode("method_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode annotation(int line, String snippet) {
        return new SyntaxNode("annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }
}
