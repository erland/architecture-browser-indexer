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

class JavaSpecialDeclarationKindContractRegressionTest extends AbstractStructuralExtractionServiceTestSupport {

    @Test
    void javaEnumExtractionKeepsClassEntityKindButAddsDeclarationKindMetadata() {
        String source = """
            package com.example.demo;
            enum Status { OPEN, CLOSED }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 1, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("enum_declaration", true, 26, 55, 1, 0, 1, 29, false, false,
                "enum Status { OPEN, CLOSED }", List.of(
                    new SyntaxNode("identifier", true, 31, 37, 1, 5, 1, 11, false, false, "Status", List.of())
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/Status.java"), "src/main/java/com/example/demo/Status.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var status = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "Status".equals(entity.name()))
            .findFirst().orElseThrow();

        assertEquals("enum", status.metadata().get("declarationKind"));
        assertEquals("com.example.demo.Status", status.metadata().get("qualifiedName"));
    }

    @Test
    void javaRecordExtractionKeepsClassEntityKindButAddsDeclarationKindMetadata() {
        String source = """
            package com.example.demo;
            record OrderRecord(String id) {}
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 1, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("record_declaration", true, 26, 58, 1, 0, 1, 32, false, false,
                "record OrderRecord(String id) {}", List.of(
                    new SyntaxNode("identifier", true, 33, 44, 1, 7, 1, 18, false, false, "OrderRecord", List.of())
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/OrderRecord.java"), "src/main/java/com/example/demo/OrderRecord.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var record = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "OrderRecord".equals(entity.name()))
            .findFirst().orElseThrow();

        assertEquals("record", record.metadata().get("declarationKind"));
        assertEquals("com.example.demo.OrderRecord", record.metadata().get("qualifiedName"));
    }

}
