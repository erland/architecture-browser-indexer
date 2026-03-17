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

class JavaFieldAndMethodDependencyContractRegressionTest extends AbstractStructuralExtractionServiceTestSupport {
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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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
