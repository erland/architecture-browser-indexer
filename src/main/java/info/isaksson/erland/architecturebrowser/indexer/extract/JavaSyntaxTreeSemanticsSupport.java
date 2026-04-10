package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaSyntaxTreeSemanticsSupport {
    private static final Set<String> JAVA_IDENTIFIER_NODE_TYPES = Set.of("identifier", "type_identifier");
    private static final Set<String> JAVA_METHOD_IDENTIFIER_NODE_TYPES = Set.of("identifier", "property_identifier");
    private static final Pattern JAVA_ANNOTATION_PATTERN = Pattern.compile("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?");
    private static final Pattern JAVA_MODIFIER_PATTERN = Pattern.compile("\\b(public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|default)\\b");
    private static final Pattern JAVA_FIELD_FALLBACK_NAME_PATTERN = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*$");
    private static final Pattern JAVA_DECLARED_TYPE_PATTERN = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^;=]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*");
    private static final Pattern JAVA_METHOD_RETURN_TYPE_PATTERN = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)$");
    private static final Pattern JAVA_PARAMETER_TYPE_PATTERN = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*$");
    private static final Pattern SIMPLE_IDENTIFIER_PATTERN = Pattern.compile("([A-Za-z_][\\w$]*)");

    private JavaSyntaxTreeSemanticsSupport() {
    }

    static String javaTypeDeclarationName(SyntaxNode node) {
        if (node == null) {
            return null;
        }
        for (SyntaxNode child : node.children()) {
            if (isJavaAnnotationOrModifierNode(child)) {
                continue;
            }
            if (JAVA_IDENTIFIER_NODE_TYPES.contains(child.type())) {
                String snippet = child.textSnippet();
                if (snippet != null && !snippet.isBlank()) {
                    return snippet;
                }
            }
        }
        for (SyntaxNode child : node.children()) {
            if (isJavaAnnotationOrModifierNode(child)) {
                continue;
            }
            Optional<SyntaxNode> nestedIdentifier = SyntaxTreeTraversalSupport.firstDescendantByType(child, JAVA_IDENTIFIER_NODE_TYPES);
            if (nestedIdentifier.isPresent()) {
                String snippet = nestedIdentifier.get().textSnippet();
                if (snippet != null && !snippet.isBlank()) {
                    return snippet;
                }
            }
        }
        return null;
    }

    static List<String> javaFieldNames(SyntaxNode node) {
        List<String> result = new ArrayList<>();
        if (node == null) {
            return List.of();
        }
        for (SyntaxNode declarator : SyntaxTreeTraversalSupport.descendantsByType(node, Set.of("variable_declarator", "constant_declarator"))) {
            String name = SyntaxTreeDeclarationSupport.declarationName(declarator);
            if (name != null && !name.isBlank()) {
                result.add(name);
            }
        }
        if (!result.isEmpty()) {
            return List.copyOf(result);
        }
        String fallback = SyntaxTreeDeclarationSupport.declarationName(node);
        if (fallback != null && !fallback.isBlank()) {
            return List.of(fallback);
        }
        if (node.textSnippet() == null || node.textSnippet().isBlank()) {
            return List.of();
        }
        String snippet = sanitizeJavaDeclarationSnippet(node.textSnippet())
            .replaceAll("\\s*=.*$", " ")
            .replaceAll(";\\s*$", " ")
            .trim();
        Matcher matcher = JAVA_FIELD_FALLBACK_NAME_PATTERN.matcher(snippet);
        return matcher.find() ? List.of(matcher.group(1)) : List.of();
    }

    static String javaFieldDeclaredType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        Matcher matcher = JAVA_DECLARED_TYPE_PATTERN.matcher(sanitizeJavaDeclarationSnippet(node.textSnippet()));
        return matcher.find() ? normalizeWhitespace(matcher.group(1)) : "";
    }

    static List<String> javaModifiers(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = JAVA_MODIFIER_PATTERN.matcher(node.textSnippet());
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result.stream().distinct().toList();
    }

    static String javaMethodLikeName(SyntaxNode node) {
        if (node == null) {
            return null;
        }
        for (SyntaxNode child : node.children()) {
            if (JAVA_METHOD_IDENTIFIER_NODE_TYPES.contains(child.type())) {
                String snippet = child.textSnippet();
                if (snippet != null && !snippet.isBlank()) {
                    return snippet;
                }
            }
        }
        if ("constructor_declaration".equals(node.type())) {
            for (SyntaxNode child : node.children()) {
                if ("type_identifier".equals(child.type())) {
                    String snippet = child.textSnippet();
                    if (snippet != null && !snippet.isBlank()) {
                        return snippet;
                    }
                }
            }
        }
        if (node.textSnippet() == null || node.textSnippet().isBlank()) {
            return SyntaxTreeDeclarationSupport.declarationName(node);
        }
        String snippet = node.textSnippet();
        int paren = snippet.lastIndexOf('(');
        String before = paren >= 0 ? snippet.substring(0, paren) : snippet;
        Matcher matcher = SIMPLE_IDENTIFIER_PATTERN.matcher(before);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    static String javaMethodReturnType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank() || !"method_declaration".equals(node.type())) {
            return "";
        }
        String methodName = javaMethodLikeName(node);
        if (methodName == null || methodName.isBlank()) {
            return "";
        }
        String sanitized = stripJavaAnnotations(node.textSnippet());
        Matcher anchor = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(").matcher(sanitized);
        if (!anchor.find()) {
            return "";
        }
        String before = sanitized.substring(0, anchor.start());
        before = stripJavaModifiers(before)
            .replaceFirst("^<[^>]+>\s*", "")
            .trim();
        Matcher matcher = JAVA_METHOD_RETURN_TYPE_PATTERN.matcher(before);
        return matcher.find() ? normalizeWhitespace(matcher.group(1)) : "";
    }

    static List<String> javaMethodParameterDeclaredTypes(SyntaxNode node) {
        String params = SyntaxTreeSnippetParsingSupport.parameterSnippet(node);
        if (params == null || params.isBlank() || "()".equals(params.strip())) {
            return List.of();
        }
        String inner = trimParameterWrapper(params);
        if (inner.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : SyntaxTreeSnippetParsingSupport.splitTopLevelCommaSeparated(inner)) {
            String type = javaParameterDeclaredType(part);
            if (!type.isBlank()) {
                result.add(type);
            }
        }
        return List.copyOf(result);
    }

    static String javaMethodDisplayName(String methodName, String parameterSnippet) {
        if (methodName == null || methodName.isBlank()) {
            return methodName;
        }
        String params = parameterSnippet == null ? "" : parameterSnippet.strip();
        return params.isEmpty() ? methodName : methodName + params;
    }

    private static String javaParameterDeclaredType(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank()) {
            return "";
        }
        String snippet = stripJavaAnnotations(parameterSnippet)
            .replaceAll("\\b(final)\\b", " ")
            .replace("...", "[]");
        Matcher matcher = JAVA_PARAMETER_TYPE_PATTERN.matcher(snippet.trim());
        return matcher.find() ? normalizeWhitespace(matcher.group(1)) : "";
    }

    private static boolean isJavaAnnotationOrModifierNode(SyntaxNode node) {
        if (node == null) {
            return false;
        }
        return switch (node.type()) {
            case "marker_annotation", "annotation", "modifiers" -> true;
            default -> false;
        };
    }

    private static String sanitizeJavaDeclarationSnippet(String snippet) {
        return stripJavaModifiers(stripJavaAnnotations(snippet))
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim();
    }

    private static String stripJavaAnnotations(String snippet) {
        return snippet == null ? "" : JAVA_ANNOTATION_PATTERN.matcher(snippet.replace('\n', ' ').replace('\r', ' ')).replaceAll(" ");
    }

    private static String stripJavaModifiers(String snippet) {
        return snippet == null ? "" : JAVA_MODIFIER_PATTERN.matcher(snippet).replaceAll(" ");
    }

    private static String trimParameterWrapper(String params) {
        String inner = params.strip();
        if (inner.startsWith("(")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        return inner;
    }

    private static String normalizeWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }
}
