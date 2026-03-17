package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.testing.fixtures.SyntaxNodeFixtureBuilder;
import info.isaksson.erland.architecturebrowser.indexer.testing.fixtures.TypeScriptExtractionFixtureBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractTypeScriptExtractionTestSupport {
    public static void assertReactHookRelationship(
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

    public static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        return TypeScriptExtractionFixtureBuilder.extract(relativePath, source, root);
    }

    public static SyntaxNode program(String source, SyntaxNode... children) {
        return SyntaxNodeFixtureBuilder.program(source, children);
    }

    public static SyntaxNode classDeclaration(int startIndex, int endIndex, int startLine, String name, List<SyntaxNode> extraChildren) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, startIndex, startIndex + name.length(), startLine, 0, startLine, name.length(), false, false, name, List.of()));
        children.addAll(extraChildren);
        return new SyntaxNode("class_declaration", true, startIndex, endIndex, startLine, 0, startLine, Math.max(0, endIndex - startIndex), false, false,
            "export class " + name + " {}", List.copyOf(children));
    }

    public static void assertAngularTemplateRelationship(
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


    public static void assertAngularFrameworkRelationship(StructuralExtractionResult result, String fromId, String toId, String label, String frameworkRelationship) {
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

    public static void assertFrontendRouteRelationship(
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

    public static void assertReactFrameworkRelationship(StructuralExtractionResult result, String fromId, String toId, String label, boolean resolved) {
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


    public static void assertAngularDiRelationship(
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


    public static void assertReactContextRelationship(
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


    public static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .sorted((left, right) -> Integer.compare(entityScore(right), entityScore(left)))
            .findFirst()
            .orElseThrow();
    }

    public static int entityScore(ExtractedEntityFact entity) {
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
