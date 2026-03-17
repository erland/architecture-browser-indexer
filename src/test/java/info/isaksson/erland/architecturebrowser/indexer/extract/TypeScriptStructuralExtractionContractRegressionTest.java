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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptStructuralExtractionContractRegressionTest extends AbstractStructuralExtractionServiceTestSupport {
    @Test
    void usesCompactDisplayNamesForTypescriptFunctions() {
        String source = """
            export function canRequestHint() { return true; }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 0, source.length(), false, false, source, List.of(
            new SyntaxNode("function_declaration", true, 0, source.length(), 0, 0, 0, source.length(), false, false, source, List.of(
                new SyntaxNode("identifier", true, 16, 30, 0, 16, 0, 30, false, false, "canRequestHint", List.of())
            ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/pages/game/useHintController.ts"), "src/pages/game/useHintController.ts", ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "canRequestHint".equals(entity.name()) && "canRequestHint".equals(entity.displayName())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS));
    }

    @Test
    void usesCompactDisplayNamesForModuleEntities() {
        String javaSource = """
            package com.example.demo;
            import com.example.shared.CustomerRepository;
            public class DemoController {}
            """;
        SyntaxNode javaRoot = new SyntaxNode("program", true, 0, javaSource.length(), 0, 0, 2, 0, false, false, javaSource, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 70, 1, 0, 1, 44, false, false, "import com.example.shared.CustomerRepository;", List.of()),
            new SyntaxNode("class_declaration", true, 71, javaSource.length(), 2, 0, 2, 30, false, false, "public class DemoController {}", List.of(
                new SyntaxNode("identifier", true, 84, 98, 2, 13, 2, 27, false, false, "DemoController", List.of())
            ))
        ));

        String javaPath = "src/main/java/com/example/demo/DemoController.java";
        SourceParseResult javaParseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(javaPath), javaPath, ParseLanguage.JAVA, javaSource),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", javaRoot, false, javaRoot.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        String yamlSource = """
            build:
              runs-on: ubuntu-latest
            """;
        SyntaxNode yamlRoot = new SyntaxNode("stream", true, 0, yamlSource.length(), 0, 0, 1, 25, false, false, yamlSource, List.of(
            new SyntaxNode("block_mapping_pair", true, 0, 5, 0, 0, 0, 5, false, false, "build", List.of())
        ));
        String yamlPath = ".github/workflows/build.yml";
        SourceParseResult yamlParseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(yamlPath), yamlPath, ParseLanguage.YAML, yamlSource),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.YAML, "tree-sitter-jtreesitter", yamlRoot, false, yamlRoot.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(javaParseResult, yamlParseResult), Map.of(ParseLanguage.JAVA, 1, ParseLanguage.YAML, 1), Map.of(ParseStatus.SUCCESS, 2)));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE
            && javaPath.equals(entity.name())
            && "DemoController.java".equals(entity.displayName())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE
            && "com.example.shared.CustomerRepository".equals(entity.name())
            && "CustomerRepository".equals(entity.displayName())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE
            && yamlPath.equals(entity.name())
            && "build.yml".equals(entity.displayName())));
    }
}
