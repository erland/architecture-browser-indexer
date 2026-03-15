package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactoryStructuralExtractionTest {
    @Test
    void includesStructuralExtractionEntitiesRelationshipsAndSummaryInDocument() {
        FileInventory inventory = new FileInventory(
            List.of(new FileInventoryEntry("src/app.ts", 10, "ts", "source", "typescript", false, List.of("typescript"))),
            1, 1, 0, Set.of("typescript"), Set.of("typescript")
        );
        String source = "import { x } from './lib';\nexport function run() { return x; }\n";

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 1, 35, false, false, source, List.of(
            new SyntaxNode("import_statement", true, 0, 26, 0, 0, 0, 26, false, false, "import { x } from './lib';", List.of()),
            new SyntaxNode("function_declaration", true, 27, source.length(), 1, 0, 1, 35, false, false,
                "export function run() { return x; }", List.of(
                    new SyntaxNode("identifier", true, 43, 46, 1, 16, 1, 19, false, false, "run", List.of())
                ))
        ));

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            List.of(new SourceParseResult(
                new SourceParseRequest(Path.of("src/app.ts"), "src/app.ts", ParseLanguage.TYPESCRIPT, source),
                ParseStatus.SUCCESS,
                new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
                List.of(),
                Map.of("parserBackend", "tree-sitter-jtreesitter"))),
            Map.of(ParseLanguage.TYPESCRIPT, 1),
            Map.of(ParseStatus.SUCCESS, 1)
        );
        StructuralExtractionResult extractionResult = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(parseBatchResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-10T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extractionResult
        );

        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "run".equals(entity.name())));
        assertTrue(document.relationships().stream().anyMatch(relationship -> "./lib".equals(relationship.label())));
        assertEquals("SUCCESS", document.runMetadata().outcome().name());
        assertTrue(document.metadata().containsKey("extractionSummary"));
    }
    @Test
    void exposesNormalizedTypeDependencyViewAndMarksImportEvidenceSeparately() {
        FileInventory inventory = new FileInventory(
            List.of(new FileInventoryEntry("src/main/java/com/example/Demo.java", 50, "java", "source", "java", false, List.of("java"))),
            1, 1, 0, Set.of("java"), Set.of("java")
        );
        String source = String.join("\n",
            "package com.example;",
            "import org.springframework.web.context.request.RequestContext;",
            "public class Demo {",
            "  private RequestContext field;",
            "  public Demo(RequestContext constructorDependency) {}",
            "  public RequestContext fetch(RequestContext request) { return request; }",
            "}",
            ""
        );

        SyntaxNode requestContextType = new SyntaxNode("type_identifier", true, 94, 108, 3, 10, 3, 24, false, false, "RequestContext", List.of());
        SyntaxNode fieldName = new SyntaxNode("identifier", true, 109, 114, 3, 25, 3, 30, false, false, "field", List.of());
        SyntaxNode fieldDeclarator = new SyntaxNode("variable_declarator", true, 94, 114, 3, 10, 3, 30, false, false, "RequestContext field", List.of(requestContextType, fieldName));
        SyntaxNode fieldDeclaration = new SyntaxNode("field_declaration", true, 86, 115, 3, 2, 3, 31, false, false, "private RequestContext field;", List.of(fieldDeclarator));

        SyntaxNode ctorType = new SyntaxNode("type_identifier", true, 128, 142, 4, 14, 4, 28, false, false, "RequestContext", List.of());
        SyntaxNode ctorParamName = new SyntaxNode("identifier", true, 143, 164, 4, 29, 4, 50, false, false, "constructorDependency", List.of());
        SyntaxNode ctorParam = new SyntaxNode("formal_parameter", true, 128, 164, 4, 14, 4, 50, false, false, "RequestContext constructorDependency", List.of(ctorType, ctorParamName));
        SyntaxNode ctorParams = new SyntaxNode("formal_parameters", true, 127, 165, 4, 13, 4, 51, false, false, "(RequestContext constructorDependency)", List.of(ctorParam));
        SyntaxNode ctorName = new SyntaxNode("identifier", true, 122, 126, 4, 8, 4, 12, false, false, "Demo", List.of());
        SyntaxNode ctorDecl = new SyntaxNode("constructor_declaration", true, 115, 169, 4, 2, 4, 55, false, false, "public Demo(RequestContext constructorDependency) {}", List.of(ctorName, ctorParams));

        SyntaxNode methodReturnType = new SyntaxNode("type_identifier", true, 179, 193, 5, 9, 5, 23, false, false, "RequestContext", List.of());
        SyntaxNode methodName = new SyntaxNode("identifier", true, 194, 199, 5, 24, 5, 29, false, false, "fetch", List.of());
        SyntaxNode methodParamType = new SyntaxNode("type_identifier", true, 200, 214, 5, 30, 5, 44, false, false, "RequestContext", List.of());
        SyntaxNode methodParamName = new SyntaxNode("identifier", true, 215, 222, 5, 45, 5, 52, false, false, "request", List.of());
        SyntaxNode methodParam = new SyntaxNode("formal_parameter", true, 200, 222, 5, 30, 5, 52, false, false, "RequestContext request", List.of(methodParamType, methodParamName));
        SyntaxNode methodParams = new SyntaxNode("formal_parameters", true, 199, 223, 5, 29, 5, 53, false, false, "(RequestContext request)", List.of(methodParam));
        SyntaxNode methodDecl = new SyntaxNode("method_declaration", true, 172, 249, 5, 2, 5, 79, false, false, "public RequestContext fetch(RequestContext request) { return request; }", List.of(methodReturnType, methodName, methodParams));

        SyntaxNode className = new SyntaxNode("identifier", true, 78, 82, 2, 13, 2, 17, false, false, "Demo", List.of());
        SyntaxNode classDecl = new SyntaxNode("class_declaration", true, 65, source.length() - 1, 2, 0, 6, 1, false, false,
            "public class Demo { ... }", List.of(className, fieldDeclaration, ctorDecl, methodDecl));

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 6, 1, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 20, 0, 0, 0, 20, false, false, "package com.example;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 19, 0, 8, 0, 19, false, false, "com.example", List.of())
            )),
            new SyntaxNode("import_declaration", true, 21, 82, 1, 0, 1, 61, false, false, "import org.springframework.web.context.request.RequestContext;", List.of(
                new SyntaxNode("scoped_identifier", true, 28, 60, 1, 7, 1, 39, false, false, "org.springframework.web.context.request.RequestContext", List.of())
            )),
            classDecl
        ));

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            List.of(new SourceParseResult(
                new SourceParseRequest(Path.of("src/main/java/com/example/Demo.java"), "src/main/java/com/example/Demo.java", ParseLanguage.JAVA, source),
                ParseStatus.SUCCESS,
                new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
                List.of(),
                Map.of("parserBackend", "tree-sitter-jtreesitter"))),
            Map.of(ParseLanguage.JAVA, 1),
            Map.of(ParseStatus.SUCCESS, 1)
        );

        StructuralExtractionResult extractionResult = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(parseBatchResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-10T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extractionResult
        );

        assertTrue(document.relationships().stream().anyMatch(relationship ->
            relationship.kind().name().equals("DEPENDS_ON")
                && "type".equals(relationship.metadata().get("dependencyView"))
                && "parameterType".equals(relationship.metadata().get("dependencySource"))
        ));
        assertTrue(document.relationships().stream().anyMatch(relationship ->
            relationship.kind().name().equals("DEPENDS_ON")
                && "evidence".equals(relationship.metadata().get("dependencyView"))
                && "import".equals(relationship.metadata().get("dependencySource"))
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> typeDependencies = (List<Map<String, Object>>) dependencyViews.get("typeDependencies");
        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "DEPENDS_ON".equals(dep.get("relationshipKind"))
                && Boolean.TRUE.equals(dep.get("externalTarget"))
                && ((List<?>) dep.get("dependencySources")).contains("field")
                && ((List<?>) dep.get("dependencySources")).contains("constructorParameter")
                && ((List<?>) dep.get("dependencySources")).contains("parameterType")
                && ((List<?>) dep.get("dependencySources")).contains("returnType")
                && Integer.valueOf(4).equals(dep.get("evidenceRelationshipCount"))
        ));
    }

}
