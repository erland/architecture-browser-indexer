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

class JavaFieldExtractionContractRegressionTest extends AbstractStructuralExtractionServiceTestSupport {

    @Test
    void javaFieldExtractionHandlesMultipleDeclarators() {
        String source = """
            package com.example.demo;
            class Demo {
                private String first, second;
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, 80, 1, 0, 3, 1, false, false,
                "class Demo { private String first, second; }", List.of(
                    new SyntaxNode("identifier", true, 32, 36, 1, 6, 1, 10, false, false, "Demo", List.of()),
                    new SyntaxNode("field_declaration", true, 45, 74, 2, 4, 2, 33, false, false,
                        "private String first, second;", List.of(
                            new SyntaxNode("type_identifier", true, 53, 59, 2, 12, 2, 18, false, false, "String", List.of()),
                            new SyntaxNode("variable_declarator", true, 60, 65, 2, 19, 2, 24, false, false, "first", List.of(
                                new SyntaxNode("identifier", true, 60, 65, 2, 19, 2, 24, false, false, "first", List.of())
                            )),
                            new SyntaxNode("variable_declarator", true, 67, 73, 2, 26, 2, 32, false, false, "second", List.of(
                                new SyntaxNode("identifier", true, 67, 73, 2, 26, 2, 32, false, false, "second", List.of())
                            ))
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/Demo.java"), "src/main/java/com/example/demo/Demo.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var fields = result.entities().stream().filter(entity -> entity.kind() == EntityKind.FIELD).toList();
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(entity -> "first".equals(entity.name()) && "String".equals(entity.metadata().get("declaredType"))));
        assertTrue(fields.stream().anyMatch(entity -> "second".equals(entity.name()) && "String".equals(entity.metadata().get("declaredType"))));
    }

}
