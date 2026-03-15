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

class StructuralExtractionServiceTest {
    @Test
    void emitsDiagnosticInsteadOfRegexFallbackForJavaWhenSyntaxTreeIsUnavailable() {
        String source = """
            package com.example.demo;
            import org.springframework.web.bind.annotation.GetMapping;
            import java.util.List;

            @RestController
            public class DemoController {
                @GetMapping("/demo")
                public String getDemo(List<String> values) {
                    return "ok";
                }
            }
            """;
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoController.java"), "src/main/java/com/example/demo/DemoController.java", ParseLanguage.JAVA, source),
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(),
            Map.of()
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)));

        assertEquals(0, result.summary().extractedByLanguage().getOrDefault("java", 0));
        assertEquals(0, result.summary().extractedByMode().getOrDefault("SOURCE_TEXT_FALLBACK", 0));
        assertTrue(result.diagnostics().stream().anyMatch(d -> "extract.java.syntax-tree-required".equals(d.code())));
    }

    @Test
    void emitsDiagnosticInsteadOfRegexFallbackForTypescriptWhenSyntaxTreeIsUnavailable() {
        String source = """
            import { Injectable } from '@nestjs/common';
            export class ApiService {}
            """;
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/app/api-service.ts"), "src/app/api-service.ts", ParseLanguage.TYPESCRIPT, source),
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(),
            Map.of()
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)));

        assertEquals(0, result.summary().extractedByLanguage().getOrDefault("typescript", 0));
        assertEquals(0, result.summary().extractedByMode().getOrDefault("SOURCE_TEXT_FALLBACK", 0));
        assertTrue(result.diagnostics().stream().anyMatch(d -> "extract.typescript.syntax-tree-required".equals(d.code())));
    }


    @Test
    void usesCompactDisplayNamesForPackagesAndFunctions() {
        String source = """
            package com.example.demo;
            public class DemoController {
                public String hello() { return "hi"; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, source.length(), 1, 0, 3, 1, false, false,
                "public class DemoController { public String hello() { return \"hi\"; } }", List.of(
                    new SyntaxNode("identifier", true, 39, 53, 1, 13, 1, 27, false, false, "DemoController", List.of()),
                    new SyntaxNode("method_declaration", true, 58, 95, 2, 4, 2, 41, false, false,
                        "public String hello() { return \"hi\"; }", List.of(
                            new SyntaxNode("identifier", true, 72, 77, 2, 18, 2, 23, false, false, "hello", List.of()),
                            new SyntaxNode("formal_parameters", true, 77, 79, 2, 23, 2, 25, false, false, "()", List.of())
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoController.java"), "src/main/java/com/example/demo/DemoController.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind().name().equals("PACKAGE")
            && "com.example.demo".equals(scope.name())
            && "demo".equals(scope.displayName())
            && scope.parentScopeId() != null
            && !"scope:repo".equals(scope.parentScopeId())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "hello".equals(entity.name()) && "hello()".equals(entity.displayName())));
    }


    @Test
    void javaMethodAndConstructorFunctionsUseActualMethodNamesAndSignatureDisplayNames() {
        String source = """
            package com.example.demo;
            public class DemoController {
                @Override
                public DemoController() {}
                public String hello() { return "hi"; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 4, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, source.length(), 1, 0, 4, 1, false, false,
                "public class DemoController { @Override public DemoController() {} public String hello() { return \"hi\"; } }", List.of(
                    new SyntaxNode("identifier", true, 39, 53, 1, 13, 1, 27, false, false, "DemoController", List.of()),
                    new SyntaxNode("constructor_declaration", true, 58, 95, 2, 4, 2, 41, false, false,
                        "@Override public DemoController() {}", List.of(
                            new SyntaxNode("marker_annotation", true, 58, 67, 2, 4, 2, 13, false, false, "@Override", List.of()),
                            new SyntaxNode("identifier", true, 75, 89, 2, 21, 2, 35, false, false, "DemoController", List.of()),
                            new SyntaxNode("formal_parameters", true, 89, 91, 2, 35, 2, 37, false, false, "()", List.of())
                        )),
                    new SyntaxNode("method_declaration", true, 96, 133, 3, 4, 3, 41, false, false,
                        "public String hello() { return \"hi\"; }", List.of(
                            new SyntaxNode("type_identifier", true, 103, 109, 3, 11, 3, 17, false, false, "String", List.of()),
                            new SyntaxNode("identifier", true, 110, 115, 3, 18, 3, 23, false, false, "hello", List.of()),
                            new SyntaxNode("formal_parameters", true, 115, 117, 3, 23, 3, 25, false, false, "()", List.of())
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoController.java"), "src/main/java/com/example/demo/DemoController.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "DemoController".equals(entity.name()) && "DemoController()".equals(entity.displayName())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "hello".equals(entity.name()) && "hello()".equals(entity.displayName())));
    }


    @Test
    void javaMethodsAndConstructorsBelongToOwningClassInsteadOfFile() {
        String source = """
            package com.example.demo;
            public class DemoController {
                public DemoController() {}
                public String hello() { return "hi"; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 4, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, source.length(), 1, 0, 4, 1, false, false,
                "public class DemoController { public DemoController() {} public String hello() { return \"hi\"; } }", List.of(
                    new SyntaxNode("identifier", true, 39, 53, 1, 13, 1, 27, false, false, "DemoController", List.of()),
                    new SyntaxNode("constructor_declaration", true, 58, 83, 2, 4, 2, 29, false, false,
                        "public DemoController() {}", List.of(
                            new SyntaxNode("identifier", true, 65, 79, 2, 11, 2, 25, false, false, "DemoController", List.of()),
                            new SyntaxNode("formal_parameters", true, 79, 81, 2, 25, 2, 27, false, false, "()", List.of())
                        )),
                    new SyntaxNode("method_declaration", true, 84, 121, 3, 4, 3, 41, false, false,
                        "public String hello() { return \"hi\"; }", List.of(
                            new SyntaxNode("type_identifier", true, 91, 97, 3, 11, 3, 17, false, false, "String", List.of()),
                            new SyntaxNode("identifier", true, 98, 103, 3, 18, 3, 23, false, false, "hello", List.of()),
                            new SyntaxNode("formal_parameters", true, 103, 105, 3, 23, 3, 25, false, false, "()", List.of())
                        ))
                ))
        ));

        String relativePath = "src/main/java/com/example/demo/DemoController.java";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String fileEntityId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.MODULE && relativePath.equals(entity.name()))
            .findFirst().orElseThrow().id();
        String classEntityId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "DemoController".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String constructorId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "DemoController".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String methodId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "hello".equals(entity.name()))
            .findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && classEntityId.equals(rel.fromEntityId()) && constructorId.equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && classEntityId.equals(rel.fromEntityId()) && methodId.equals(rel.toEntityId())));
        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && fileEntityId.equals(rel.fromEntityId()) && constructorId.equals(rel.toEntityId())));
        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && fileEntityId.equals(rel.fromEntityId()) && methodId.equals(rel.toEntityId())));
    }

    @Test
    void javaNestedTypeMethodsBelongToNearestOwningType() {
        String source = """
            package com.example.demo;
            public class Outer {
                public void outerMethod() {}
                class Inner {
                    Inner() {}
                    void innerMethod() {}
                }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 6, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, source.length(), 1, 0, 6, 1, false, false,
                "public class Outer { public void outerMethod() {} class Inner { Inner() {} void innerMethod() {} } }", List.of(
                    new SyntaxNode("identifier", true, 39, 44, 1, 13, 1, 18, false, false, "Outer", List.of()),
                    new SyntaxNode("method_declaration", true, 49, 78, 2, 4, 2, 33, false, false,
                        "public void outerMethod() {}", List.of(
                            new SyntaxNode("identifier", true, 61, 72, 2, 16, 2, 27, false, false, "outerMethod", List.of()),
                            new SyntaxNode("formal_parameters", true, 72, 74, 2, 27, 2, 29, false, false, "()", List.of())
                        )),
                    new SyntaxNode("class_declaration", true, 83, 130, 3, 4, 5, 5, false, false,
                        "class Inner { Inner() {} void innerMethod() {} }", List.of(
                            new SyntaxNode("identifier", true, 89, 94, 3, 10, 3, 15, false, false, "Inner", List.of()),
                            new SyntaxNode("constructor_declaration", true, 101, 111, 4, 8, 4, 18, false, false,
                                "Inner() {}", List.of(
                                    new SyntaxNode("identifier", true, 101, 106, 4, 8, 4, 13, false, false, "Inner", List.of()),
                                    new SyntaxNode("formal_parameters", true, 106, 108, 4, 13, 4, 15, false, false, "()", List.of())
                                )),
                            new SyntaxNode("method_declaration", true, 120, 141, 5, 8, 5, 29, false, false,
                                "void innerMethod() {}", List.of(
                                    new SyntaxNode("identifier", true, 125, 136, 5, 13, 5, 24, false, false, "innerMethod", List.of()),
                                    new SyntaxNode("formal_parameters", true, 136, 138, 5, 24, 5, 26, false, false, "()", List.of())
                                ))
                        ))
                ))
        ));

        String relativePath = "src/main/java/com/example/demo/Outer.java";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String outerClassId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "Outer".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String innerClassId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "Inner".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String outerMethodId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "outerMethod".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String innerCtorId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "Inner".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String innerMethodId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "innerMethod".equals(entity.name()))
            .findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && outerClassId.equals(rel.fromEntityId()) && outerMethodId.equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && innerClassId.equals(rel.fromEntityId()) && innerCtorId.equals(rel.toEntityId())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && innerClassId.equals(rel.fromEntityId()) && innerMethodId.equals(rel.toEntityId())));
    }

    @Test
    void javaInterfaceMethodsBelongToOwningInterface() {
        String source = """
            package com.example.demo;
            public interface GreetingApi {
                String hello();
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("interface_declaration", true, 26, source.length(), 1, 0, 3, 1, false, false,
                "public interface GreetingApi { String hello(); }", List.of(
                    new SyntaxNode("identifier", true, 43, 54, 1, 17, 1, 28, false, false, "GreetingApi", List.of()),
                    new SyntaxNode("method_declaration", true, 59, 74, 2, 4, 2, 19, false, false,
                        "String hello();", List.of(
                            new SyntaxNode("type_identifier", true, 59, 65, 2, 4, 2, 10, false, false, "String", List.of()),
                            new SyntaxNode("identifier", true, 66, 71, 2, 11, 2, 16, false, false, "hello", List.of()),
                            new SyntaxNode("formal_parameters", true, 71, 73, 2, 16, 2, 18, false, false, "()", List.of())
                        ))
                ))
        ));

        String relativePath = "src/main/java/com/example/demo/GreetingApi.java";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String interfaceId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.INTERFACE && "GreetingApi".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String methodId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "hello".equals(entity.name()))
            .findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && interfaceId.equals(rel.fromEntityId()) && methodId.equals(rel.toEntityId())));
    }

    @Test
    void fileScopesUseContainingDirectoryAsParentAndCompactDisplayName() {
        String source = """
            export function canRequestHint() { return true; }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 0, source.length(), false, false, source, List.of(
            new SyntaxNode("function_declaration", true, 0, source.length(), 0, 0, 0, source.length(), false, false, source, List.of(
                new SyntaxNode("identifier", true, 16, 30, 0, 16, 0, 30, false, false, "canRequestHint", List.of())
            ))
        ));

        String relativePath = "src/pages/game/useHintController.ts";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.scopes().stream().anyMatch(scope -> scope.kind().name().equals("FILE")
            && relativePath.equals(scope.name())
            && "useHintController.ts".equals(scope.displayName())
            && IdUtils.scopeId("directory", "src/pages/game").equals(scope.parentScopeId())));
    }


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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(javaParseResult, yamlParseResult), Map.of(ParseLanguage.JAVA, 1, ParseLanguage.YAML, 1), Map.of(ParseStatus.SUCCESS, 2)));

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

    @Test
    void prefersSyntaxTreeModeWhenRealSyntaxTreeIsAvailable() {
        String source = """
            package com.example.demo;
            import java.util.List;
            public class DemoController {
                public String hello() { return "hi"; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 4, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 48, 1, 0, 1, 22, false, false, "import java.util.List;", List.of()),
            new SyntaxNode("class_declaration", true, 49, source.length(), 2, 0, 4, 1, false, false,
                "public class DemoController { public String hello() { return \"hi\"; } }", List.of(
                    new SyntaxNode("identifier", true, 62, 76, 2, 13, 2, 27, false, false, "DemoController", List.of()),
                    new SyntaxNode("method_declaration", true, 81, 118, 3, 4, 3, 41, false, false,
                        "public String hello() { return \"hi\"; }", List.of(
                            new SyntaxNode("identifier", true, 95, 100, 3, 18, 3, 23, false, false, "hello", List.of()),
                            new SyntaxNode("formal_parameters", true, 100, 102, 3, 23, 3, 25, false, false, "()", List.of())
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoController.java"), "src/main/java/com/example/demo/DemoController.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertEquals(1, result.summary().extractedByMode().get("SYNTAX_TREE"));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS && "DemoController".equals(entity.name())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "hello".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && "java.util.List".equals(rel.label())));
    }


    @Test
    void javaClassExtendsClassProducesExtendsRelationshipToDeclaredType() {
        String source = """
            package com.example.demo;
            class Base {}
            class Derived extends Base {}
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 2, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, 39, 1, 0, 1, 13, false, false,
                "class Base {}", List.of(
                    new SyntaxNode("identifier", true, 32, 36, 1, 6, 1, 10, false, false, "Base", List.of())
                )),
            new SyntaxNode("class_declaration", true, 40, 68, 2, 0, 2, 28, false, false,
                "class Derived extends Base {}", List.of(
                    new SyntaxNode("identifier", true, 46, 53, 2, 6, 2, 13, false, false, "Derived", List.of()),
                    new SyntaxNode("type_identifier", true, 62, 66, 2, 22, 2, 26, false, false, "Base", List.of())
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/Derived.java"), "src/main/java/com/example/demo/Derived.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String baseId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "Base".equals(entity.name()))
            .findFirst().orElseThrow().id();
        String derivedId = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "Derived".equals(entity.name()))
            .findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && derivedId.equals(rel.fromEntityId())
            && baseId.equals(rel.toEntityId())
            && "com.example.demo.Base".equals(rel.label())));
    }

    @Test
    void javaClassImplementsMultipleInterfacesProducesImplementsRelationships() {
        String source = """
            package com.example.demo;
            interface Alpha {}
            interface Beta {}
            class Demo implements Alpha, Beta {}
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("interface_declaration", true, 26, 44, 1, 0, 1, 18, false, false,
                "interface Alpha {}", List.of(
                    new SyntaxNode("identifier", true, 36, 41, 1, 10, 1, 15, false, false, "Alpha", List.of())
                )),
            new SyntaxNode("interface_declaration", true, 45, 62, 2, 0, 2, 17, false, false,
                "interface Beta {}", List.of(
                    new SyntaxNode("identifier", true, 55, 59, 2, 10, 2, 14, false, false, "Beta", List.of())
                )),
            new SyntaxNode("class_declaration", true, 63, 98, 3, 0, 3, 35, false, false,
                "class Demo implements Alpha, Beta {}", List.of(
                    new SyntaxNode("identifier", true, 69, 73, 3, 6, 3, 10, false, false, "Demo", List.of()),
                    new SyntaxNode("type_identifier", true, 85, 90, 3, 22, 3, 27, false, false, "Alpha", List.of()),
                    new SyntaxNode("type_identifier", true, 92, 96, 3, 29, 3, 33, false, false, "Beta", List.of())
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/Demo.java"), "src/main/java/com/example/demo/Demo.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String demoId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.CLASS && "Demo".equals(entity.name())).findFirst().orElseThrow().id();
        String alphaId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.INTERFACE && "Alpha".equals(entity.name())).findFirst().orElseThrow().id();
        String betaId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.INTERFACE && "Beta".equals(entity.name())).findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS && demoId.equals(rel.fromEntityId()) && alphaId.equals(rel.toEntityId()) && "com.example.demo.Alpha".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS && demoId.equals(rel.fromEntityId()) && betaId.equals(rel.toEntityId()) && "com.example.demo.Beta".equals(rel.label())));
    }

    @Test
    void javaInterfaceExtendsMultipleInterfacesProducesExtendsRelationships() {
        String source = """
            package com.example.demo;
            interface ParentOne {}
            interface ParentTwo {}
            interface Child extends ParentOne, ParentTwo {}
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("interface_declaration", true, 26, 48, 1, 0, 1, 22, false, false,
                "interface ParentOne {}", List.of(
                    new SyntaxNode("identifier", true, 36, 45, 1, 10, 1, 19, false, false, "ParentOne", List.of())
                )),
            new SyntaxNode("interface_declaration", true, 49, 71, 2, 0, 2, 22, false, false,
                "interface ParentTwo {}", List.of(
                    new SyntaxNode("identifier", true, 59, 68, 2, 10, 2, 19, false, false, "ParentTwo", List.of())
                )),
            new SyntaxNode("interface_declaration", true, 72, 118, 3, 0, 3, 46, false, false,
                "interface Child extends ParentOne, ParentTwo {}", List.of(
                    new SyntaxNode("identifier", true, 82, 87, 3, 10, 3, 15, false, false, "Child", List.of()),
                    new SyntaxNode("type_identifier", true, 96, 105, 3, 24, 3, 33, false, false, "ParentOne", List.of()),
                    new SyntaxNode("type_identifier", true, 107, 116, 3, 35, 3, 44, false, false, "ParentTwo", List.of())
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/Child.java"), "src/main/java/com/example/demo/Child.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String childId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.INTERFACE && "Child".equals(entity.name())).findFirst().orElseThrow().id();
        String parentOneId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.INTERFACE && "ParentOne".equals(entity.name())).findFirst().orElseThrow().id();
        String parentTwoId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.INTERFACE && "ParentTwo".equals(entity.name())).findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS && childId.equals(rel.fromEntityId()) && parentOneId.equals(rel.toEntityId()) && "com.example.demo.ParentOne".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS && childId.equals(rel.fromEntityId()) && parentTwoId.equals(rel.toEntityId()) && "com.example.demo.ParentTwo".equals(rel.label())));
    }

    @Test
    void javaInheritanceResolvesImportedSupertypesAsDeterministicInferredTargets() {
        String source = """
            package com.example.demo;
            import java.util.ArrayList;
            public class DemoList extends ArrayList<String> {}
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 2, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 53, 1, 0, 1, 27, false, false, "import java.util.ArrayList;", List.of()),
            new SyntaxNode("class_declaration", true, 54, 98, 2, 0, 2, 44, false, false,
                "public class DemoList extends ArrayList<String> {}", List.of(
                    new SyntaxNode("identifier", true, 67, 75, 2, 13, 2, 21, false, false, "DemoList", List.of()),
                    new SyntaxNode("generic_type", true, 84, 101, 2, 30, 2, 47, false, false, "ArrayList<String>", List.of(
                        new SyntaxNode("type_identifier", true, 84, 93, 2, 30, 2, 39, false, false, "ArrayList", List.of()),
                        new SyntaxNode("type_identifier", true, 94, 100, 2, 40, 2, 46, false, false, "String", List.of())
                    ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoList.java"), "src/main/java/com/example/demo/DemoList.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String demoListId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.CLASS && "DemoList".equals(entity.name())).findFirst().orElseThrow().id();
        var arrayListEntity = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "java.util.ArrayList".equals(entity.name()))
            .findFirst().orElseThrow();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS && demoListId.equals(rel.fromEntityId()) && arrayListEntity.id().equals(rel.toEntityId()) && "java.util.ArrayList".equals(rel.label())));
    }

    @Test
    void javaClassFieldsAreExtractedAndBelongToOwningClass() {
        String source = """
            package com.example.demo;
            class Demo {
                @Inject
                private final Repository repo;
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 3, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("class_declaration", true, 26, 88, 1, 0, 3, 1, false, false,
                "class Demo { @Inject private final Repository repo; }", List.of(
                    new SyntaxNode("identifier", true, 32, 36, 1, 6, 1, 10, false, false, "Demo", List.of()),
                    new SyntaxNode("field_declaration", true, 45, 81, 2, 4, 2, 40, false, false,
                        "@Inject private final Repository repo;", List.of(
                            new SyntaxNode("marker_annotation", true, 45, 52, 2, 4, 2, 11, false, false, "@Inject", List.of()),
                            new SyntaxNode("type_identifier", true, 67, 77, 2, 26, 2, 36, false, false, "Repository", List.of()),
                            new SyntaxNode("variable_declarator", true, 78, 82, 2, 37, 2, 41, false, false, "repo", List.of(
                                new SyntaxNode("identifier", true, 78, 82, 2, 37, 2, 41, false, false, "repo", List.of())
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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var demo = result.entities().stream().filter(entity -> entity.kind() == EntityKind.CLASS && "Demo".equals(entity.name())).findFirst().orElseThrow();
        var repo = result.entities().stream().filter(entity -> entity.kind() == EntityKind.FIELD && "repo".equals(entity.name())).findFirst().orElseThrow();

        assertEquals("repo", repo.displayName());
        assertEquals("Repository", repo.metadata().get("declaredType"));
        assertEquals("com.example.demo.Demo", repo.metadata().get("ownerQualifiedName"));
        assertEquals(List.of("Inject"), repo.metadata().get("annotations"));
        assertEquals(List.of("private", "final"), repo.metadata().get("modifiers"));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS && demo.id().equals(rel.fromEntityId()) && repo.id().equals(rel.toEntityId())));
    }

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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var fields = result.entities().stream().filter(entity -> entity.kind() == EntityKind.FIELD).toList();
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(entity -> "first".equals(entity.name()) && "String".equals(entity.metadata().get("declaredType"))));
        assertTrue(fields.stream().anyMatch(entity -> "second".equals(entity.name()) && "String".equals(entity.metadata().get("declaredType"))));
    }



    @Test
    void javaTypeDependenciesIncludeFieldAndMethodSignaturesBeyondImports() {
        String source = """
            package com.example.demo;
            import java.util.List;
            import java.time.Instant;
            import com.example.shared.RequestContext;
            class Dependency {}
            class Demo extends Dependency {
                private List<RequestContext> contexts;
                Demo(RequestContext context) {}
                public Dependency find(RequestContext context, List<Instant> instants) { return null; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 7, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 48, 1, 0, 1, 22, false, false, "import java.util.List;", List.of()),
            new SyntaxNode("import_declaration", true, 49, 74, 2, 0, 2, 25, false, false, "import java.time.Instant;", List.of()),
            new SyntaxNode("import_declaration", true, 75, 116, 3, 0, 3, 41, false, false, "import com.example.shared.RequestContext;", List.of()),
            new SyntaxNode("class_declaration", true, 117, 135, 4, 0, 4, 18, false, false,
                "class Dependency {}", List.of(
                    new SyntaxNode("identifier", true, 123, 133, 4, 6, 4, 16, false, false, "Dependency", List.of())
                )),
            new SyntaxNode("class_declaration", true, 136, source.length(), 5, 0, 7, 1, false, false,
                "class Demo extends Dependency { private List<RequestContext> contexts; Demo(RequestContext context) {} public Dependency find(RequestContext context, List<Instant> instants) { return null; } }", List.of(
                    new SyntaxNode("identifier", true, 142, 146, 5, 6, 5, 10, false, false, "Demo", List.of()),
                    new SyntaxNode("type_identifier", true, 155, 165, 5, 19, 5, 29, false, false, "Dependency", List.of()),
                    new SyntaxNode("field_declaration", true, 168, 206, 6, 4, 6, 42, false, false,
                        "private List<RequestContext> contexts;", List.of(
                            new SyntaxNode("generic_type", true, 176, 196, 6, 12, 6, 32, false, false, "List<RequestContext>", List.of()),
                            new SyntaxNode("variable_declarator", true, 197, 205, 6, 33, 6, 41, false, false, "contexts", List.of(
                                new SyntaxNode("identifier", true, 197, 205, 6, 33, 6, 41, false, false, "contexts", List.of())
                            ))
                        )),
                    new SyntaxNode("constructor_declaration", true, 207, 239, 6, 43, 6, 75, false, false,
                        "Demo(RequestContext context) {}", List.of(
                            new SyntaxNode("identifier", true, 207, 211, 6, 43, 6, 47, false, false, "Demo", List.of()),
                            new SyntaxNode("formal_parameters", true, 211, 235, 6, 47, 6, 71, false, false, "(RequestContext context)", List.of())
                        )),
                    new SyntaxNode("method_declaration", true, 240, 324, 7, 4, 7, 88, false, false,
                        "public Dependency find(RequestContext context, List<Instant> instants) { return null; }", List.of(
                            new SyntaxNode("type_identifier", true, 247, 257, 7, 11, 7, 21, false, false, "Dependency", List.of()),
                            new SyntaxNode("identifier", true, 258, 262, 7, 22, 7, 26, false, false, "find", List.of()),
                            new SyntaxNode("formal_parameters", true, 262, 314, 7, 26, 7, 78, false, false, "(RequestContext context, List<Instant> instants)", List.of())
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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String demoId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.CLASS && "Demo".equals(entity.name())).findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && demoId.equals(rel.fromEntityId()) && "com.example.demo.Dependency".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && demoId.equals(rel.fromEntityId()) && "java.util.List".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && demoId.equals(rel.fromEntityId()) && "com.example.shared.RequestContext".equals(rel.label())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && demoId.equals(rel.fromEntityId()) && "java.time.Instant".equals(rel.label())));
    }

    @Test
    void javaMethodMetadataCapturesReturnAndParameterTypesForDeclarationDependencies() {
        String source = """
            package com.example.demo;
            import java.time.Instant;
            class Demo {
                public Response handle(Request request, Instant at) { return null; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 4, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 51, 1, 0, 1, 25, false, false, "import java.time.Instant;", List.of()),
            new SyntaxNode("class_declaration", true, 52, source.length(), 2, 0, 4, 1, false, false,
                "class Demo { public Response handle(Request request, Instant at) { return null; } }", List.of(
                    new SyntaxNode("identifier", true, 58, 62, 2, 6, 2, 10, false, false, "Demo", List.of()),
                    new SyntaxNode("method_declaration", true, 65, 142, 3, 4, 3, 81, false, false,
                        "public Response handle(Request request, Instant at) { return null; }", List.of(
                            new SyntaxNode("type_identifier", true, 72, 80, 3, 11, 3, 19, false, false, "Response", List.of()),
                            new SyntaxNode("identifier", true, 81, 87, 3, 20, 3, 26, false, false, "handle", List.of()),
                            new SyntaxNode("formal_parameters", true, 87, 115, 3, 26, 3, 54, false, false, "(Request request, Instant at)", List.of())
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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var handle = result.entities().stream().filter(entity -> entity.kind() == EntityKind.FUNCTION && "handle".equals(entity.name())).findFirst().orElseThrow();
        assertEquals("Response", handle.metadata().get("returnType"));
        assertEquals(List.of("Request", "Instant"), handle.metadata().get("parameterTypes"));
    }


    @Test
    void javaDependencyRelationshipsCarrySourceAndCategoryMetadata() {
        String source = """
            package com.example.demo;
            import java.util.List;
            import com.example.shared.RequestContext;
            class Dependency {}
            class Demo extends Dependency {
                private List<RequestContext> contexts;
                Demo(RequestContext context) {}
                public Dependency find(RequestContext context) { return null; }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 7, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 48, 1, 0, 1, 22, false, false, "import java.util.List;", List.of()),
            new SyntaxNode("import_declaration", true, 49, 90, 2, 0, 2, 41, false, false, "import com.example.shared.RequestContext;", List.of()),
            new SyntaxNode("class_declaration", true, 91, 109, 3, 0, 3, 18, false, false,
                "class Dependency {}", List.of(
                    new SyntaxNode("identifier", true, 97, 107, 3, 6, 3, 16, false, false, "Dependency", List.of())
                )),
            new SyntaxNode("class_declaration", true, 110, source.length(), 4, 0, 7, 1, false, false,
                "class Demo extends Dependency { private List<RequestContext> contexts; Demo(RequestContext context) {} public Dependency find(RequestContext context) { return null; } }", List.of(
                    new SyntaxNode("identifier", true, 116, 120, 4, 6, 4, 10, false, false, "Demo", List.of()),
                    new SyntaxNode("type_identifier", true, 129, 139, 4, 19, 4, 29, false, false, "Dependency", List.of()),
                    new SyntaxNode("field_declaration", true, 142, 180, 5, 4, 5, 42, false, false,
                        "private List<RequestContext> contexts;", List.of(
                            new SyntaxNode("generic_type", true, 150, 170, 5, 12, 5, 32, false, false, "List<RequestContext>", List.of()),
                            new SyntaxNode("variable_declarator", true, 171, 179, 5, 33, 5, 41, false, false, "contexts", List.of(
                                new SyntaxNode("identifier", true, 171, 179, 5, 33, 5, 41, false, false, "contexts", List.of())
                            ))
                        )),
                    new SyntaxNode("constructor_declaration", true, 181, 213, 6, 4, 6, 36, false, false,
                        "Demo(RequestContext context) {}", List.of(
                            new SyntaxNode("identifier", true, 181, 185, 6, 4, 6, 8, false, false, "Demo", List.of()),
                            new SyntaxNode("formal_parameters", true, 185, 209, 6, 8, 6, 32, false, false, "(RequestContext context)", List.of())
                        )),
                    new SyntaxNode("method_declaration", true, 214, 281, 7, 4, 7, 71, false, false,
                        "public Dependency find(RequestContext context) { return null; }", List.of(
                            new SyntaxNode("type_identifier", true, 221, 231, 7, 11, 7, 21, false, false, "Dependency", List.of()),
                            new SyntaxNode("identifier", true, 232, 236, 7, 22, 7, 26, false, false, "find", List.of()),
                            new SyntaxNode("formal_parameters", true, 236, 260, 7, 26, 7, 50, false, false, "(RequestContext context)", List.of())
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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String demoId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.CLASS && "Demo".equals(entity.name())).findFirst().orElseThrow().id();
        String fileEntityId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.MODULE && entity.name().toString().endsWith("Demo.java")).findFirst().orElseThrow().id();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && fileEntityId.equals(rel.fromEntityId())
            && "java.util.List".equals(rel.label())
            && "import".equals(rel.metadata().get("dependencySource"))
            && "evidence".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && demoId.equals(rel.fromEntityId())
            && "java.util.List".equals(rel.label())
            && "field".equals(rel.metadata().get("dependencySource"))
            && "composition".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && demoId.equals(rel.fromEntityId())
            && "com.example.shared.RequestContext".equals(rel.label())
            && "constructorParameter".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && demoId.equals(rel.fromEntityId())
            && "com.example.demo.Dependency".equals(rel.label())
            && "returnType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && demoId.equals(rel.fromEntityId())
            && "com.example.shared.RequestContext".equals(rel.label())
            && "parameterType".equals(rel.metadata().get("dependencySource"))
            && "api".equals(rel.metadata().get("dependencyCategory"))));
    }

    @Test
    void javaHierarchyRelationshipsCarrySourceAndCategoryMetadata() {
        String source = """
            package com.example.demo;
            interface BasePort {}
            interface ExtendedPort extends BasePort {}
            class BaseService {}
            class DemoService extends BaseService implements ExtendedPort {}
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 4, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("interface_declaration", true, 26, 47, 1, 0, 1, 21, false, false, "interface BasePort {}", List.of(
                new SyntaxNode("identifier", true, 36, 44, 1, 10, 1, 18, false, false, "BasePort", List.of())
            )),
            new SyntaxNode("interface_declaration", true, 48, 89, 2, 0, 2, 41, false, false, "interface ExtendedPort extends BasePort {}", List.of(
                new SyntaxNode("identifier", true, 58, 70, 2, 10, 2, 22, false, false, "ExtendedPort", List.of()),
                new SyntaxNode("type_identifier", true, 79, 87, 2, 31, 2, 39, false, false, "BasePort", List.of())
            )),
            new SyntaxNode("class_declaration", true, 90, 111, 3, 0, 3, 21, false, false, "class BaseService {}", List.of(
                new SyntaxNode("identifier", true, 96, 107, 3, 6, 3, 17, false, false, "BaseService", List.of())
            )),
            new SyntaxNode("class_declaration", true, 112, 171, 4, 0, 4, 59, false, false,
                "class DemoService extends BaseService implements ExtendedPort {}", List.of(
                    new SyntaxNode("identifier", true, 118, 129, 4, 6, 4, 17, false, false, "DemoService", List.of()),
                    new SyntaxNode("type_identifier", true, 138, 149, 4, 26, 4, 37, false, false, "BaseService", List.of()),
                    new SyntaxNode("type_identifier", true, 161, 173, 4, 49, 4, 61, false, false, "ExtendedPort", List.of())
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/demo/DemoService.java"), "src/main/java/com/example/demo/DemoService.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && "com.example.demo.BaseService".equals(rel.label())
            && "extends".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.IMPLEMENTS
            && "com.example.demo.ExtendedPort".equals(rel.label())
            && "implements".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && "com.example.demo.BaseService".equals(rel.label())
            && "extends".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && "com.example.demo.ExtendedPort".equals(rel.label())
            && "implements".equals(rel.metadata().get("dependencySource"))
            && "hierarchy".equals(rel.metadata().get("dependencyCategory"))));
    }

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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        var record = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "OrderRecord".equals(entity.name()))
            .findFirst().orElseThrow();

        assertEquals("record", record.metadata().get("declarationKind"));
        assertEquals("com.example.demo.OrderRecord", record.metadata().get("qualifiedName"));
    }


    @Test
    void extractsJaxRsResourcesEndpointsAndParameterKindsFromJavaSyntaxTree() {
        String source = """
            package com.example.orders.api;
            @Path("/orders")
            public class OrderResource {
                @GET
                @Path("/{id}")
                public OrderDto getOrder(@PathParam("id") String id, @QueryParam("expand") boolean expand) {
                    return null;
                }
            }
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 7, 0, false, false, source, List.of(
            new SyntaxNode("package_declaration", true, 0, 31, 0, 0, 0, 31, false, false, "package com.example.orders.api;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 30, 0, 8, 0, 30, false, false, "com.example.orders.api", List.of())
            )),
            new SyntaxNode("class_declaration", true, 32, source.length(), 1, 0, 7, 0, false, false,
                "@Path(\"/orders\") public class OrderResource { @GET @Path(\"/{id}\") public OrderDto getOrder(@PathParam(\"id\") String id, @QueryParam(\"expand\") boolean expand) { return null; } }",
                List.of(
                    new SyntaxNode("annotation", true, 32, 48, 1, 0, 1, 16, false, false, "@Path(\"/orders\")", List.of()),
                    new SyntaxNode("identifier", true, 62, 75, 2, 17, 2, 30, false, false, "OrderResource", List.of()),
                    new SyntaxNode("method_declaration", true, 78, source.length()-2, 3, 4, 6, 5, false, false,
                        "@GET @Path(\"/{id}\") public OrderDto getOrder(@PathParam(\"id\") String id, @QueryParam(\"expand\") boolean expand) { return null; }",
                        List.of(
                            new SyntaxNode("marker_annotation", true, 78, 82, 3, 4, 3, 8, false, false, "@GET", List.of()),
                            new SyntaxNode("annotation", true, 83, 97, 4, 4, 4, 18, false, false, "@Path(\"/{id}\")", List.of()),
                            new SyntaxNode("type_identifier", true, 105, 113, 5, 11, 5, 19, false, false, "OrderDto", List.of()),
                            new SyntaxNode("identifier", true, 114, 122, 5, 20, 5, 28, false, false, "getOrder", List.of()),
                            new SyntaxNode("formal_parameters", true, 122, 192, 5, 28, 5, 98, false, false, "(@PathParam(\"id\") String id, @QueryParam(\"expand\") boolean expand)", List.of())
                        ))
                ))
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("src/main/java/com/example/orders/api/OrderResource.java"), "src/main/java/com/example/orders/api/OrderResource.java", ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS
            && "OrderResource".equals(entity.name())
            && Boolean.TRUE.equals(entity.metadata().get("jaxRsResource"))
            && "/orders".equals(entity.metadata().get("jaxRsBasePath"))));

        var endpoint = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.ENDPOINT)
            .findFirst()
            .orElseThrow();
        assertEquals("GET /orders/{id}", endpoint.name());
        assertEquals("GET", endpoint.metadata().get("httpMethod"));
        assertEquals("/orders/{id}", endpoint.metadata().get("path"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> parameterDetails = (List<Map<String, String>>) endpoint.metadata().get("parameterDetails");
        assertEquals(List.of("PATH", "QUERY"), parameterDetails.stream().map(item -> item.get("parameterKind")).toList());

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXPOSES && "GET /orders/{id}".equals(rel.label())));
    }

}
