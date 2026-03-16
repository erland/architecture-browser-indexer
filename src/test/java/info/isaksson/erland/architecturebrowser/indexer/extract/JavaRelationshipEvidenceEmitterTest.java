package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaRelationshipEvidenceEmitterTest {

    private final JavaEntityMapper entityMapper = new JavaEntityMapper();
    private final JavaRelationshipEvidenceEmitter emitter = new JavaRelationshipEvidenceEmitter();

    @Test
    void emitsHierarchyAndDeclaredTypeDependenciesUsingCurrentResolutionRules() {
        String source = "package com.example.orders; class OrderResource extends BaseResource implements OrderApi { private OrderRepository repository; }";
        SourceParseResult parseResult = parseResult("src/main/java/com/example/orders/OrderResource.java", source);
        SyntaxNode typeNode = node("class_declaration", "class OrderResource extends BaseResource implements OrderApi { }", 1, 1,
            node("identifier", "OrderResource", 1, 1)
        );
        ExtractedEntityFact typeEntity = entityMapper.toTypeEntity(
            parseResult,
            parseResult.request().relativePath(),
            "com.example.orders",
            ExtractionMode.SYNTAX_TREE,
            "scope:package:com.example.orders",
            typeNode,
            null
        );

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        accumulator.addEntity(typeEntity);
        Map<String, JavaDeclaredType> declaredTypes = Map.of(
            "OrderResource", new JavaDeclaredType(typeEntity.id(), "com.example.orders.OrderResource", typeEntity.kind()),
            "com.example.orders.OrderResource", new JavaDeclaredType(typeEntity.id(), "com.example.orders.OrderResource", typeEntity.kind())
        );

        emitter.addTypeRelationships(
            accumulator,
            parseResult.request().relativePath(),
            "com.example.orders",
            typeNode,
            typeEntity,
            Map.of("BaseResource", "com.example.shared.BaseResource", "OrderApi", "com.example.api.OrderApi"),
            declaredTypes
        );
        emitter.addDeclaredTypeDependencies(
            accumulator,
            typeEntity.id(),
            List.of("List<OrderRepository>"),
            parseResult.request().relativePath(),
            "com.example.orders",
            1,
            typeEntity.sourceRefs().getFirst(),
            Map.of("OrderRepository", "com.example.orders.OrderRepository"),
            declaredTypes,
            emitter.dependencyMetadata("field", "composition")
        );

        List<ExtractedRelationshipFact> relationships = accumulator.relationships();
        Set<String> labels = relationships.stream().map(ExtractedRelationshipFact::label).collect(java.util.stream.Collectors.toSet());
        assertTrue(labels.contains("com.example.shared.BaseResource"));
        assertTrue(labels.contains("com.example.api.OrderApi"));
        assertTrue(labels.contains("com.example.orders.OrderRepository"));
        assertTrue(relationships.stream().anyMatch(rel -> "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(relationships.stream().anyMatch(rel -> "composition".equals(rel.metadata().get("dependencyCategory"))));
        assertEquals("import-or-package", accumulator.entities().stream()
            .filter(entity -> "com.example.api.OrderApi".equals(entity.name()))
            .findFirst()
            .orElseThrow()
            .metadata()
            .get("resolution"));
    }

    private static SourceParseResult parseResult(String relativePath, String source) {
        SyntaxNode root = node("program", source, 0, 1);
        SourceParseRequest request = new SourceParseRequest(null, relativePath, ParseLanguage.JAVA, source);
        SyntaxTree tree = new SyntaxTree(ParseLanguage.JAVA, "test", root, false, root.nodeCount());
        return new SourceParseResult(request, ParseStatus.SUCCESS, tree, List.of(), Map.of());
    }

    private static SyntaxNode node(String type, String text, int startLine, int endLine, SyntaxNode... children) {
        return new SyntaxNode(type, true, 0, text.length(), startLine, 0, endLine, text.length(), false, false, text, List.of(children));
    }
}
