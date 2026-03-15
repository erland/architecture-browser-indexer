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


}
