package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntaxTreeExtractionSupportModularizationTest {

    @Test
    void traversalSupportFindsDescendantsByType() {
        SyntaxNode root = node("program", "program", 0,
            node("class_declaration", "class Foo {}", 1,
                node("identifier", "Foo", 1),
                node("method_declaration", "void bar() {}", 2,
                    node("identifier", "bar", 2)
                )
            )
        );

        List<SyntaxNode> matches = SyntaxTreeTraversalSupport.findAllByType(root, Set.of("identifier"));

        assertEquals(List.of("Foo", "bar"), matches.stream().map(SyntaxNode::textSnippet).toList());
        assertTrue(SyntaxTreeTraversalSupport.containsDescendantType(root, "method_declaration"));
    }

    @Test
    void facadeDelegatesJavaAndTypeScriptSemanticsToSpecializedHelpers() {
        SyntaxNode javaMethod = node("method_declaration", "public List<String> findAll(String id, int count) {}", 4,
            node("identifier", "findAll", 4),
            node("formal_parameters", "(String id, int count)", 4)
        );
        SyntaxNode typeScriptProperty = node("public_field_definition", "readonly value?: Promise<Result> = getValue();", 7,
            node("property_identifier", "value", 7)
        );

        assertEquals("findAll", SyntaxTreeExtractionSupport.javaMethodLikeName(javaMethod));
        assertEquals("List<String>", SyntaxTreeExtractionSupport.javaMethodReturnType(javaMethod));
        assertEquals(List.of("String", "int"), SyntaxTreeExtractionSupport.javaMethodParameterDeclaredTypes(javaMethod));
        assertEquals("Promise<Result>", SyntaxTreeExtractionSupport.typeScriptDeclaredType(typeScriptProperty));
        assertTrue(SyntaxTreeExtractionSupport.typeScriptOptional(typeScriptProperty));
        assertTrue(SyntaxTreeExtractionSupport.typeScriptReadonly(typeScriptProperty));
    }

    private static SyntaxNode node(String type, String snippet, int startLine, SyntaxNode... children) {
        return new SyntaxNode(
            type,
            true,
            0,
            snippet == null ? 0 : snippet.length(),
            Math.max(0, startLine - 1),
            0,
            Math.max(0, startLine - 1),
            snippet == null ? 0 : snippet.length(),
            false,
            false,
            snippet,
            List.of(children)
        );
    }
}
