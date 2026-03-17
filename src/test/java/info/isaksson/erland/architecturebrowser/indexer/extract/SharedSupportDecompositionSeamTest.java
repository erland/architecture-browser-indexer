package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedSupportDecompositionSeamTest {

    @Test
    void syntaxTreeSupportsPreserveNavigationAndAnnotationContracts() {
        SyntaxNode annotation = new SyntaxNode("annotation", true, 0, 12, 0, 0, 0, 12, false, false, "@Component()", List.of());
        SyntaxNode identifier = new SyntaxNode("type_identifier", true, 13, 23, 0, 13, 0, 23, false, false, "OrderPage", List.of());
        SyntaxNode root = new SyntaxNode("class_declaration", true, 0, 23, 0, 0, 0, 23, false, false, "@Component() OrderPage", List.of(annotation, identifier));

        assertEquals(List.of(annotation), SyntaxTreeNavigationSupport.childrenByType(root, "annotation"));
        assertEquals(identifier, SyntaxTreeNavigationSupport.firstChildByType(root, "type_identifier").orElseThrow());
        assertEquals(List.of(identifier), SyntaxTreeNavigationSupport.descendantsByType(root, Set.of("type_identifier")));
        assertEquals(List.of("Component"), SyntaxTreeAnnotationSupport.extractAnnotationsFromSnippet("@Component()"));
        assertTrue(SyntaxTreeAnnotationSupport.isFrameworkAnnotation("Component"));
        assertEquals("OrderPage", SyntaxTreeAnnotationSupport.findTypeName(root, "type_identifier").orElseThrow());
    }

    @Test
    void extractionSupportsPreserveEntityAndRelationshipFactories() {
        var typeEntity = ExtractionEntitySupport.inferredTypeEntity(
            "java",
            EntityKind.CLASS,
            "com.example.OrderService",
            "src/main/java/com/example/OrderService.java",
            3,
            Map.of("external", true)
        );
        assertEquals("com.example.OrderService", typeEntity.name());
        assertEquals(EntityKind.CLASS, typeEntity.kind());
        assertEquals(true, typeEntity.metadata().get("external"));

        var ref = ExtractionRelationshipSupport.sourceRef(
            "src/main/java/com/example/OrderService.java",
            8,
            "OrderRepository repository;",
            Map.of("language", "java")
        );
        var rel = ExtractionRelationshipSupport.typedRelationship(
            RelationshipKind.DEPENDS_ON,
            "dependsOn",
            "entity:source",
            "entity:target",
            "OrderRepository",
            ref,
            "java",
            Map.of("dependencySource", "field")
        );
        assertEquals(RelationshipKind.DEPENDS_ON, rel.kind());
        assertEquals("entity:source", rel.fromEntityId());
        assertEquals("entity:target", rel.toEntityId());
        assertEquals("field", rel.metadata().get("dependencySource"));
    }
}
