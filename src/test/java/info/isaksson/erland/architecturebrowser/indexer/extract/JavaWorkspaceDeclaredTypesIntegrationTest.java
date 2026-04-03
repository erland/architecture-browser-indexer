package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
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

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.annotation;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaWorkspaceDeclaredTypesIntegrationTest {

    @Test
    void resolvesCrossFileJpaAssociationsToObservedEntitiesInsteadOfInferredDuplicates() {
        StructuralExtractionService service = new StructuralExtractionService(
            new StructuralExtractorRegistry(List.of(new JavaStructuralExtractor()))
        );
        StructuralExtractionResult result = service.extract(new ParseBatchResult(
            List.of(projectParseResult(), taskParseResult()),
            Map.of(ParseLanguage.JAVA, 2),
            Map.of(ParseStatus.SUCCESS, 2)
        ));

        var projectEntity = result.entities().stream()
            .filter(entity -> "com.example.domain.Project".equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
        var taskEntity = result.entities().stream()
            .filter(entity -> "com.example.domain.Task".equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();

        var association = result.relationships().stream()
            .filter(relationship -> projectEntity.id().equals(relationship.fromEntityId()))
            .filter(relationship -> "association".equals(relationship.metadata().get("associationKind")))
            .findFirst()
            .orElseThrow();

        assertEquals(taskEntity.id(), association.toEntityId());
        assertNotNull(result.entities().stream()
            .filter(entity -> taskEntity.id().equals(entity.id()))
            .findFirst()
            .orElse(null));
        assertEquals(0, result.entities().stream()
            .filter(entity -> Boolean.TRUE.equals(entity.metadata().get("external")))
            .filter(entity -> "com.example.domain.Task".equals(entity.metadata().get("qualifiedName")))
            .count());
    }

    private static SourceParseResult projectParseResult() {
        String relativePath = "src/main/java/com/example/domain/Project.java";
        String source = "package com.example.domain; @Entity class Project { @OneToMany List<Task> tasks; }";
        return new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source,
                packageDecl(0, "package com.example.domain;", "com.example.domain"),
                classDecl(
                    1,
                    "Project",
                    "@Entity class Project { @OneToMany List<Task> tasks; }",
                    annotation(1, "@Entity"),
                    fieldDecl(
                        1,
                        "@OneToMany List<Task> tasks;",
                        "List<Task>",
                        "tasks",
                        annotation(1, "@OneToMany")
                    )
                )
            ), false, 2),
            List.<ParseIssue>of(),
            Map.of()
        );
    }

    private static SourceParseResult taskParseResult() {
        String relativePath = "src/main/java/com/example/domain/Task.java";
        String source = "package com.example.domain; @Entity class Task {}";
        return new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.JAVA, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", program(source,
                packageDecl(0, "package com.example.domain;", "com.example.domain"),
                classDecl(1, "Task", "@Entity class Task {}", annotation(1, "@Entity"))
            ), false, 2),
            List.<ParseIssue>of(),
            Map.of()
        );
    }
}
