package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractTypeScriptExtractionTestSupport {
    protected static void assertReactHookRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String consumerKind,
        String hookClassification,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && "usesHook".equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElseThrow();
        assertEquals("react:uses-hook", relationship.metadata().get("dependencySource"));
        assertEquals(consumerKind, relationship.metadata().get("hookConsumerKind"));
        assertEquals(hookClassification, relationship.metadata().get("hookClassification"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromReactHookExtraction"));
    }

    protected static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));
    }

    protected static SyntaxNode program(String source, SyntaxNode... children) {
        int endLine = Math.max(0, source.split("\\R", -1).length - 1);
        int endColumn = source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1;
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, endLine, endColumn, false, false, source, List.of(children));
    }

    protected static SyntaxNode classDeclaration(int startIndex, int endIndex, int startLine, String name, List<SyntaxNode> extraChildren) {
        int startColumn = 0;
        int endColumn = Math.max(0, endIndex - startIndex);
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, startIndex, startIndex + name.length(), startLine, startColumn, startLine, startColumn + name.length(), false, false, name, List.of()));
        children.addAll(extraChildren);
        return new SyntaxNode("class_declaration", true, startIndex, endIndex, startLine, startColumn, startLine, endColumn, false, false,
            "export class " + name + " {}", List.copyOf(children));
    }



    protected static void assertAngularTemplateRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        String expectedSource = switch (frameworkRelationship) {
            case "templateRenders" -> "angular:template-renders";
            case "usesDirective" -> "angular:template-uses-directive";
            case "usesPipe" -> "angular:template-uses-pipe";
            default -> throw new IllegalArgumentException(frameworkRelationship);
        };
        assertEquals(expectedSource, relationship.metadata().get("dependencySource"));
        assertEquals(Boolean.TRUE, relationship.metadata().get("resolvedFromAngularTemplateExtraction"));
    }


    protected static void assertAngularFrameworkRelationship(StructuralExtractionResult result, String fromId, String toId, String label, String frameworkRelationship) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("angular:" + frameworkRelationship, relationship.metadata().get("dependencySource"));
    }

    protected static void assertFrontendRouteRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String framework,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && framework.equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals(framework + ":route-" + frameworkRelationship, relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromRouteExtraction"));
    }

    protected static void assertReactFrameworkRelationship(StructuralExtractionResult result, String fromId, String toId, String label, boolean resolved) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && "renders".equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("react:jsx-renders", relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromJsxComposition"));
    }


    protected static void assertAngularDiRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("angular:" + frameworkRelationship, relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromAngularDiExtraction"));
    }


    protected static void assertReactContextRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("providesContext".equals(frameworkRelationship) ? "react:provides-context" : "react:consumes-context", relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromReactContextExtraction"));
    }


    protected static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .sorted((left, right) -> Integer.compare(entityScore(right), entityScore(left)))
            .findFirst()
            .orElseThrow();
    }

    protected static int entityScore(ExtractedEntityFact entity) {
        int score = 0;
        if (Boolean.TRUE.equals(entity.metadata().get("reactContext"))) {
            score += 10;
        }
        if (Boolean.TRUE.equals(entity.metadata().get("declaredReactContext"))) {
            score += 5;
        }
        if (Boolean.FALSE.equals(entity.metadata().get("external"))) {
            score += 2;
        }
        return score;
    }

}
