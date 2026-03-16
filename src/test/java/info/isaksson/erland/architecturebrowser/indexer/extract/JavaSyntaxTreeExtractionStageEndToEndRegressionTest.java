package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaSyntaxTreeExtractionStageEndToEndRegressionTest {

    @Test
    void javaStageStillProducesArchitectFacingSemanticsEndToEnd() {
        String relativePath = "src/main/java/com/example/orders/OrderResource.java";
        String source = "package com.example.orders; @Path(\"/orders\") class OrderResource { Event<OrderCreated> events; OrderRepository repository; @POST OrderEntity create(OrderEntity request) { events.fire(new OrderCreated()); repository.save(request); return new OrderEntity(); } } @Entity class OrderEntity {}";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(
                ParseLanguage.JAVA,
                "tree-sitter-jtreesitter",
                program(
                    source,
                    packageDecl(0, "package com.example.orders;", "com.example.orders"),
                    classDecl(
                        1,
                        "OrderResource",
                        "@Path(\"/orders\") class OrderResource { Event<OrderCreated> events; OrderRepository repository; @POST OrderEntity create(OrderEntity request) { events.fire(new OrderCreated()); repository.save(request); return new OrderEntity(); } }",
                        annotation(1, "@Path(\"/orders\")"),
                        fieldDecl(1, "Event<OrderCreated> events;", "Event<OrderCreated>", "events"),
                        fieldDecl(1, "OrderRepository repository;", "OrderRepository", "repository"),
                        methodDecl(1, "@POST OrderEntity create(OrderEntity request) { events.fire(new OrderCreated()); repository.save(request); return new OrderEntity(); }", "OrderEntity", "create", "(OrderEntity request)", annotation(1, "@POST"))
                    ),
                    classDecl(2, "OrderEntity", "@Entity class OrderEntity {}", annotation(2, "@Entity"))
                ),
                false,
                1
            ),
            List.<ParseIssue>of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        ExtractionAccumulator accumulator = new JavaStructuralExtractor().extract(parseResult, new ExtractionAccumulator());

        ExtractedEntityFact method = accumulator.entities().stream()
            .filter(entity -> "create".equals(entity.name()))
            .findFirst()
            .orElseThrow();

        assertEquals(Boolean.TRUE, method.metadata().get("jaxRsEndpoint"));
        assertEquals(Boolean.TRUE, method.metadata().get("writePath"));
        assertEquals("com.example.orders.OrderCreated", method.metadata().get("cdiPublishedEventType"));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> String.valueOf(rel.metadata().getOrDefault("dependencyCategory", "")).equals("api")));
        assertTrue(accumulator.extractedByMode().containsKey(ExtractionMode.SYNTAX_TREE.name()));
    }
}
