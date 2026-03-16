package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
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

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.program;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.typeIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaTypeDeclarationFlowTest {

    @Test
    void mapsTypeEntityAndReturnsUpdatedOwnership() throws Exception {
        String relativePath = "src/main/java/com/example/orders/OrderService.java";
        String packageName = "com.example.orders";
        String source = "package com.example.orders; class OrderService extends BaseService implements OrdersPort {}";
        SyntaxNode node = classDecl(1, "OrderService", "class OrderService extends BaseService implements OrdersPort {}",
            typeIdentifier(1, "BaseService"),
            typeIdentifier(1, "OrdersPort")
        );
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source, packageDecl(0, "package com.example.orders;", packageName), node), false, 1),
            List.of(),
            Map.of()
        );
        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        JavaExtractionContext context = new JavaExtractionContext(relativePath, packageName, source, Map.of(), Map.of());

        JavaSyntaxTreeExtractionStage stage = new JavaSyntaxTreeExtractionStage();
        Field field = JavaSyntaxTreeExtractionStage.class.getDeclaredField("typeDeclarationFlow");
        field.setAccessible(true);
        JavaTypeDeclarationFlow flow = (JavaTypeDeclarationFlow) field.get(stage);

        JavaTypeTraversalResult result = flow.handleTypeNode(new JavaTypeNodeRequest(
            parseResult,
            accumulator,
            relativePath,
            packageName,
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            "entity:file:OrderService.java",
            node,
            JavaOwnerContext.root(),
            Map.of(),
            Map.of(),
            context
        ));

        assertTrue(result.handled());
        assertEquals("class OrderService extends BaseService implements OrdersPort {}", result.owningTypeSnippet());
        assertEquals("com.example.orders.OrderService", result.owningQualifiedName());
        var entity = classByQualifiedName(accumulator, "com.example.orders.OrderService");
        assertEquals(entity.id(), result.owningTypeEntityId());
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && "entity:file:OrderService.java".equals(rel.fromEntityId()) && entity.id().equals(rel.toEntityId())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS && entity.id().equals(rel.fromEntityId())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS && entity.id().equals(rel.fromEntityId())));
    }
}
