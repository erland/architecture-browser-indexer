package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaTraversalNodeDispatchFlowTest {

    @Test
    void dispatchesTypeNodesWithOwnerHandoffAndKeepsOwnerForMembers() throws Exception {
        String relativePath = "src/main/java/com/example/orders/OrderResource.java";
        String source = "package com.example.orders; @Path(\"/orders\") class OrderResource { CustomerEntity customer; @POST OrderCreated create(OrderRequest request) { return new OrderCreated(); } }";
        SyntaxNode typeNode = classDecl(
            1,
            "OrderResource",
            "@Path(\"/orders\") class OrderResource { CustomerEntity customer; @POST OrderCreated create(OrderRequest request) { return new OrderCreated(); } }",
            annotation(1, "@Path(\"/orders\")"),
            fieldDecl(1, "CustomerEntity customer;", "CustomerEntity", "customer"),
            methodDecl(1, "@POST OrderCreated create(OrderRequest request) { return new OrderCreated(); }", "OrderCreated", "create", "(OrderRequest request)", annotation(1, "@POST"))
        );
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source, packageDecl(0, "package com.example.orders;", "com.example.orders"), typeNode), false, 1),
            List.<ParseIssue>of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        JavaSyntaxTreeExtractionStage stage = new JavaSyntaxTreeExtractionStage();
        Field field = JavaSyntaxTreeExtractionStage.class.getDeclaredField("traversalNodeDispatchFlow");
        field.setAccessible(true);
        JavaTraversalNodeDispatchFlow flow = (JavaTraversalNodeDispatchFlow) field.get(stage);

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        JavaExtractionContext context = new JavaExtractionContext(relativePath, "com.example.orders", source, Map.of(), Map.of(), Map.of());

        JavaSyntaxTreeTraversal.JavaTraversalOwnership rootOwnership = new JavaSyntaxTreeTraversal.JavaTraversalOwnership(null, null, null);
        JavaNodeDispatchResult typeDispatch = flow.handleNode(
            parseResult,
            accumulator,
            relativePath,
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:java-package:com.example.orders",
            "scope:file:src/main/java/com/example/orders/OrderResource.java",
            "entity:file:src/main/java/com/example/orders/OrderResource.java",
            typeNode,
            rootOwnership,
            Map.of(),
            Map.of(),
            context
        );

        JavaSyntaxTreeTraversal.JavaTraversalOwnership typeOwnership = typeDispatch.ownership();
        assertTrue(typeDispatch.handled());
        assertTrue(typeDispatch.typeTraversalResult().handled());
        assertEquals("com.example.orders.OrderResource", typeOwnership.owningQualifiedName());
        assertNotNull(typeOwnership.owningTypeEntityId());

        SyntaxNode fieldNode = typeNode.children().stream().filter(child -> "field_declaration".equals(child.type())).findFirst().orElseThrow();
        JavaNodeDispatchResult fieldDispatch = flow.handleNode(
            parseResult,
            accumulator,
            relativePath,
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:java-package:com.example.orders",
            "scope:file:src/main/java/com/example/orders/OrderResource.java",
            "entity:file:src/main/java/com/example/orders/OrderResource.java",
            fieldNode,
            typeOwnership,
            Map.of(),
            Map.of(),
            context
        );
        JavaSyntaxTreeTraversal.JavaTraversalOwnership afterField = fieldDispatch.ownership();
        assertTrue(fieldDispatch.handled());
        assertTrue(fieldDispatch.memberExtractionResult().handled());
        assertEquals(typeOwnership.owningQualifiedName(), afterField.owningQualifiedName());
        assertEquals(1, fieldDispatch.memberExtractionResult().emittedEntityIds().size());
        assertTrue(accumulator.entities().stream().anyMatch(entity -> "customer".equals(entity.name())));

        SyntaxNode methodNode = typeNode.children().stream().filter(child -> "method_declaration".equals(child.type())).findFirst().orElseThrow();
        JavaNodeDispatchResult methodDispatch = flow.handleNode(
            parseResult,
            accumulator,
            relativePath,
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:java-package:com.example.orders",
            "scope:file:src/main/java/com/example/orders/OrderResource.java",
            "entity:file:src/main/java/com/example/orders/OrderResource.java",
            methodNode,
            typeOwnership,
            Map.of(),
            Map.of(),
            context
        );
        JavaSyntaxTreeTraversal.JavaTraversalOwnership afterMethod = methodDispatch.ownership();
        assertTrue(methodDispatch.handled());
        assertTrue(methodDispatch.memberExtractionResult().handled());
        assertEquals(typeOwnership.owningQualifiedName(), afterMethod.owningQualifiedName());
        assertEquals(1, methodDispatch.memberExtractionResult().emittedEntityIds().size());
        assertTrue(accumulator.entities().stream().anyMatch(entity -> "create".equals(entity.name()) && Boolean.TRUE.equals(entity.metadata().get("jaxRsEndpoint"))));
    }
}
