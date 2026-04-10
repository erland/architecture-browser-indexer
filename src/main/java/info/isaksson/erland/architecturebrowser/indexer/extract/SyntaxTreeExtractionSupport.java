package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SyntaxTreeExtractionSupport {
    private SyntaxTreeExtractionSupport() {
    }

    static List<SyntaxNode> findAllByType(SyntaxNode root, Set<String> types) {
        return SyntaxTreeTraversalSupport.findAllByType(root, types);
    }

    static Optional<SyntaxNode> firstDescendantByType(SyntaxNode node, Set<String> types) {
        return SyntaxTreeTraversalSupport.firstDescendantByType(node, types);
    }

    static List<SyntaxNode> descendantsByType(SyntaxNode node, Set<String> types) {
        return SyntaxTreeTraversalSupport.descendantsByType(node, types);
    }

    static Optional<String> extractQualifiedName(String snippet) {
        return SyntaxTreeSnippetParsingSupport.extractQualifiedName(snippet);
    }

    static List<String> extractAnnotationsFromSnippet(String snippet) {
        return SyntaxTreeSnippetParsingSupport.extractAnnotationsFromSnippet(snippet);
    }

    static int oneBasedLine(SyntaxNode node) {
        return SyntaxTreeDeclarationSupport.oneBasedLine(node);
    }

    static String declarationName(SyntaxNode node) {
        return SyntaxTreeDeclarationSupport.declarationName(node);
    }

    static String javaTypeDeclarationName(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaTypeDeclarationName(node);
    }

    static String parameterSnippet(SyntaxNode node) {
        return SyntaxTreeSnippetParsingSupport.parameterSnippet(node);
    }

    static List<String> javaFieldNames(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaFieldNames(node);
    }

    static String javaFieldDeclaredType(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaFieldDeclaredType(node);
    }

    static List<String> javaModifiers(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaModifiers(node);
    }

    static String javaMethodLikeName(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaMethodLikeName(node);
    }

    static String javaMethodReturnType(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaMethodReturnType(node);
    }

    static List<String> javaMethodParameterDeclaredTypes(SyntaxNode node) {
        return JavaSyntaxTreeSemanticsSupport.javaMethodParameterDeclaredTypes(node);
    }

    static String javaMethodDisplayName(String methodName, String parameterSnippet) {
        return JavaSyntaxTreeSemanticsSupport.javaMethodDisplayName(methodName, parameterSnippet);
    }

    static String typeScriptDeclaredType(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptDeclaredType(node);
    }

    static List<String> typeScriptModifiers(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptModifiers(node);
    }

    static String typeScriptAccessibility(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptAccessibility(node);
    }

    static boolean typeScriptOptional(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptOptional(node);
    }

    static boolean typeScriptReadonly(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptReadonly(node);
    }

    static boolean isTypeScriptMethodLikeDeclaration(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.isTypeScriptMethodLikeDeclaration(node);
    }

    static boolean isTypeScriptPropertyLikeDeclaration(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.isTypeScriptPropertyLikeDeclaration(node);
    }

    static String typeScriptMethodReturnType(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptMethodReturnType(node);
    }

    static List<String> typeScriptMethodParameterDeclaredTypes(SyntaxNode node) {
        return TypeScriptSyntaxTreeSemanticsSupport.typeScriptMethodParameterDeclaredTypes(node);
    }

    static boolean containsDescendantType(SyntaxNode node, String type) {
        return SyntaxTreeTraversalSupport.containsDescendantType(node, type);
    }
}
