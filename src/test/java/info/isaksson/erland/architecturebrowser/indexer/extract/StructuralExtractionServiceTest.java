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
}
