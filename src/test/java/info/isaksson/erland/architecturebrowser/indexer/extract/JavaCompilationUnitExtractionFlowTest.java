package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.importDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaCompilationUnitExtractionFlowTest {

    @Test
    void extractsCompilationUnitScopesImportsAndObservedTypes() throws Exception {
        String relativePath = "src/main/java/com/example/orders/OrderResource.java";
        String source = "package com.example.orders; import jakarta.ws.rs.POST; class OrderResource {}";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(
                ParseLanguage.JAVA,
                "tree-sitter-jtreesitter",
                program(
                    source,
                    packageDecl(0, "package com.example.orders;", "com.example.orders"),
                    importDecl(0, "import jakarta.ws.rs.POST;"),
                    classDecl(1, "OrderResource", "class OrderResource {}")
                ),
                false,
                1
            ),
            List.<ParseIssue>of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        JavaSyntaxTreeExtractionStage stage = new JavaSyntaxTreeExtractionStage();
        Field field = JavaSyntaxTreeExtractionStage.class.getDeclaredField("compilationUnitExtractionFlow");
        field.setAccessible(true);
        JavaCompilationUnitExtractionFlow flow = (JavaCompilationUnitExtractionFlow) field.get(stage);

        ExtractionAccumulator accumulator = flow.extractCompilationUnit(parseResult, new ExtractionAccumulator(), relativePath, parseResult.syntaxTree());

        assertEquals(1, accumulator.filesExtracted());
        assertTrue(accumulator.scopes().stream().anyMatch(scope -> "src/main/java/com/example/orders/OrderResource.java".equals(scope.name())
            || "src/main/java/com/example/orders/OrderResource.java".equals(String.valueOf(scope.metadata().get("relativePath")))));
        assertTrue(accumulator.scopes().stream().anyMatch(scope -> "scope:java-package:com.example.orders".equals(scope.id())
            || "com.example.orders".equals(scope.name())
            || "com.example.orders".equals(String.valueOf(scope.metadata().get("packageName")))));
        assertTrue(accumulator.entities().stream().anyMatch(entity -> "MODULE".equals(String.valueOf(entity.kind()))
            && ("src/main/java/com/example/orders/OrderResource.java".equals(entity.name())
            || "src/main/java/com/example/orders/OrderResource.java".equals(String.valueOf(entity.metadata().get("relativePath"))))));
        assertTrue(accumulator.entities().stream().anyMatch(entity -> Boolean.TRUE.equals(entity.metadata().get("external")) && "jakarta.ws.rs.POST".equals(entity.metadata().get("qualifiedName"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> IdUtils.fileEntityId(relativePath).equals(rel.fromEntityId())
            && "import".equals(String.valueOf(rel.metadata().get("dependencySource")))));
        assertEquals("com.example.orders.OrderResource", classByQualifiedName(accumulator, "com.example.orders.OrderResource").metadata().get("qualifiedName"));
    }
}
