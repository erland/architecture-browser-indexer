package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptSyntaxTreeSemanticsSupport {
    private static final Pattern TYPESCRIPT_DECORATOR_PATTERN = Pattern.compile("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?");
    private static final Pattern TYPESCRIPT_MODIFIER_PATTERN = Pattern.compile("\\b(public|private|protected|static|abstract|readonly|declare|override|export)\\b");
    private static final Pattern TYPESCRIPT_ACCESSIBILITY_PATTERN = Pattern.compile("\\b(public|private|protected)\\b");
    private static final Set<String> TYPESCRIPT_METHOD_LIKE_DECLARATIONS = Set.of(
        "method_definition", "method_signature", "abstract_method_signature"
    );
    private static final Set<String> TYPESCRIPT_PROPERTY_LIKE_DECLARATIONS = Set.of(
        "public_field_definition", "property_signature", "abstract_property_signature", "field_definition"
    );

    private TypeScriptSyntaxTreeSemanticsSupport() {
    }

    static String typeScriptDeclaredType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        String snippet = stripTypeScriptDecorators(node.textSnippet());
        int colon = snippet.indexOf(':');
        if (colon < 0) {
            return "";
        }
        return extractTrailingTypeSnippet(snippet.substring(colon + 1), "=", ";", "{", "}");
    }

    static List<String> typeScriptModifiers(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = TYPESCRIPT_MODIFIER_PATTERN.matcher(node.textSnippet());
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result.stream().distinct().toList();
    }

    static String typeScriptAccessibility(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        Matcher matcher = TYPESCRIPT_ACCESSIBILITY_PATTERN.matcher(node.textSnippet());
        return matcher.find() ? matcher.group(1) : "";
    }

    static boolean typeScriptOptional(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return false;
        }
        String name = SyntaxTreeDeclarationSupport.declarationName(node);
        if (name == null || name.isBlank()) {
            return false;
        }
        String snippet = node.textSnippet().replace('\n', ' ').replace('\r', ' ');
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\?").matcher(snippet).find();
    }

    static boolean typeScriptReadonly(SyntaxNode node) {
        return typeScriptModifiers(node).contains("readonly");
    }

    static boolean isTypeScriptMethodLikeDeclaration(SyntaxNode node) {
        return node != null && TYPESCRIPT_METHOD_LIKE_DECLARATIONS.contains(node.type());
    }

    static boolean isTypeScriptPropertyLikeDeclaration(SyntaxNode node) {
        return node != null && TYPESCRIPT_PROPERTY_LIKE_DECLARATIONS.contains(node.type());
    }

    static String typeScriptMethodReturnType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        String snippet = stripTypeScriptDecorators(node.textSnippet());
        int closeParen = snippet.indexOf(')');
        if (closeParen < 0) {
            return "";
        }
        String tail = snippet.substring(closeParen + 1).trim();
        if (!tail.startsWith(":")) {
            return "";
        }
        return extractTrailingTypeSnippet(tail.substring(1), "{", ";", "=");
    }

    static List<String> typeScriptMethodParameterDeclaredTypes(SyntaxNode node) {
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
            String type = typeScriptParameterDeclaredType(part);
            if (!type.isBlank()) {
                result.add(type);
            }
        }
        return List.copyOf(result);
    }

    private static String typeScriptParameterDeclaredType(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank()) {
            return "";
        }
        String snippet = stripTypeScriptDecorators(parameterSnippet);
        int colon = snippet.indexOf(':');
        if (colon < 0) {
            return "";
        }
        return extractTrailingTypeSnippet(snippet.substring(colon + 1), "=", ",");
    }

    private static String extractTrailingTypeSnippet(String tail, String... markers) {
        String trimmedTail = tail.trim();
        int cut = trimmedTail.length();
        for (String marker : markers) {
            int idx = trimmedTail.indexOf(marker);
            if (idx >= 0) {
                cut = Math.min(cut, idx);
            }
        }
        return trimmedTail.substring(0, cut).replaceAll("\\s+", " ").trim();
    }

    private static String stripTypeScriptDecorators(String snippet) {
        return snippet == null ? "" : TYPESCRIPT_DECORATOR_PATTERN.matcher(snippet.replace('\n', ' ').replace('\r', ' ')).replaceAll(" ").trim();
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
}
