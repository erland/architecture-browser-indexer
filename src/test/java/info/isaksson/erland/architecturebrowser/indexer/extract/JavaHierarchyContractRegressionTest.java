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

class JavaHierarchyContractRegressionTest extends AbstractStructuralExtractionServiceTestSupport {
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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

        String demoListId = result.entities().stream().filter(entity -> entity.kind() == EntityKind.CLASS && "DemoList".equals(entity.name())).findFirst().orElseThrow().id();
        var arrayListEntity = result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && "java.util.ArrayList".equals(entity.name()))
            .findFirst().orElseThrow();

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS && demoListId.equals(rel.fromEntityId()) && arrayListEntity.id().equals(rel.toEntityId()) && "java.util.ArrayList".equals(rel.label())));
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

        StructuralExtractionResult result = extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));

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
}
