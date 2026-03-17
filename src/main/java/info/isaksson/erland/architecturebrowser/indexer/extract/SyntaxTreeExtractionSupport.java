package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SyntaxTreeExtractionSupport {
    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile("([A-Za-z_][\\w.]*\\*?)");
    private static final Pattern ANNOTATION_NAME_PATTERN = Pattern.compile("@([A-Za-z_][\\w.]*)");

    private SyntaxTreeExtractionSupport() {
    }

    static List<SyntaxNode> findAllByType(SyntaxNode root, Set<String> types) {
        List<SyntaxNode> result = new ArrayList<>();
        if (root != null) {
            visit(root, node -> {
                if (types.contains(node.type())) {
                    result.add(node);
                }
            });
        }
        return List.copyOf(result);
    }

    static Optional<SyntaxNode> firstDescendantByType(SyntaxNode node, Set<String> types) {
        if (node == null) {
            return Optional.empty();
        }
        if (types.contains(node.type())) {
            return Optional.of(node);
        }
        for (SyntaxNode child : node.children()) {
            Optional<SyntaxNode> found = firstDescendantByType(child, types);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    static List<SyntaxNode> descendantsByType(SyntaxNode node, Set<String> types) {
        List<SyntaxNode> result = new ArrayList<>();
        if (node != null) {
            visit(node, candidate -> {
                if (types.contains(candidate.type())) {
                    result.add(candidate);
                }
            });
        }
        return List.copyOf(result);
    }

    static Optional<String> extractQualifiedName(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = QUALIFIED_NAME_PATTERN.matcher(snippet);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return Optional.ofNullable(last);
    }

    static List<String> extractAnnotationsFromSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = ANNOTATION_NAME_PATTERN.matcher(snippet);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return List.copyOf(result);
    }

    static int oneBasedLine(SyntaxNode node) {
        return SyntaxTreeNavigationSupport.oneBasedLine(node);
    }

    static String declarationName(SyntaxNode node) {
        return firstDescendantByType(node, Set.of("identifier", "type_identifier", "property_identifier"))
            .map(SyntaxNode::textSnippet)
            .orElse(null);
    }

    static String parameterSnippet(SyntaxNode node) {
        String extracted = firstDescendantByType(node, Set.of("formal_parameters", "parameters"))
            .map(SyntaxNode::textSnippet)
            .orElse("");
        if (!extracted.isBlank()) {
            return extracted;
        }
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        String snippet = node.textSnippet();
        int start = snippet.indexOf('(');
        if (start < 0) {
            return "";
        }
        int depth = 0;
        for (int i = start; i < snippet.length(); i++) {
            char ch = snippet.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return snippet.substring(start, i + 1).trim();
                }
            }
        }
        return "";
    }

    static List<String> javaFieldNames(SyntaxNode node) {
        List<String> result = new ArrayList<>();
        if (node == null) {
            return List.of();
        }
        for (SyntaxNode declarator : descendantsByType(node, Set.of("variable_declarator", "constant_declarator"))) {
            String name = declarationName(declarator);
            if (name != null && !name.isBlank()) {
                result.add(name);
            }
        }
        if (!result.isEmpty()) {
            return List.copyOf(result);
        }
        String fallback = declarationName(node);
        if (fallback != null && !fallback.isBlank()) {
            return List.of(fallback);
        }
        if (node.textSnippet() == null || node.textSnippet().isBlank()) {
            return List.of();
        }
        String snippet = node.textSnippet().replace('\n', ' ').replace('\r', ' ').trim();
        snippet = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        snippet = snippet.replaceAll("\\b(public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|default)\\b", " ");
        snippet = snippet.replaceAll("\\s*=.*$", " ");
        snippet = snippet.replaceAll(";\\s*$", " ").trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*$").matcher(snippet);
        return matcher.find() ? List.of(matcher.group(1)) : List.of();
    }

    static String javaFieldDeclaredType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        String snippet = node.textSnippet().replace('\n', ' ').replace('\r', ' ').trim();
        snippet = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        snippet = snippet.replaceAll("\\b(public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|default)\\b", " ");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^;=]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*").matcher(snippet);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    static List<String> javaModifiers(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b(public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|default)\\b").matcher(node.textSnippet());
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
            if (Set.of("identifier", "property_identifier").contains(child.type())) {
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
            return declarationName(node);
        }
        String snippet = node.textSnippet();
        int paren = snippet.lastIndexOf('(');
        String before = paren >= 0 ? snippet.substring(0, paren) : snippet;
        Matcher matcher = Pattern.compile("([A-Za-z_][\\w$]*)").matcher(before);
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
        String snippet = node.textSnippet().replace('\n', ' ').replace('\r', ' ').trim();
        String sanitized = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        Matcher anchor = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(").matcher(sanitized);
        if (!anchor.find()) {
            return "";
        }
        String before = sanitized.substring(0, anchor.start())
            .replaceAll("<[^>]+>\\s*", " ")
            .replaceAll("\\b(public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|default)\\b", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)$").matcher(before);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    static List<String> javaMethodParameterDeclaredTypes(SyntaxNode node) {
        String params = parameterSnippet(node);
        if (params == null || params.isBlank() || "()".equals(params.strip())) {
            return List.of();
        }
        String inner = params.strip();
        if (inner.startsWith("(")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        if (inner.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
            String type = javaParameterDeclaredType(part);
            if (!type.isBlank()) {
                result.add(type);
            }
        }
        return List.copyOf(result);
    }

    private static String javaParameterDeclaredType(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank()) {
            return "";
        }
        String snippet = parameterSnippet.replace('\n', ' ').replace('\r', ' ').trim();
        snippet = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        snippet = snippet.replaceAll("\\b(final)\\b", " ");
        snippet = snippet.replace("...", "[]");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*$").matcher(snippet);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    private static List<String> splitTopLevelCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<') {
                depth++;
            } else if (ch == '>') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ',' && depth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
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



    static String typeScriptDeclaredType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        String snippet = node.textSnippet().replace('\n', ' ').replace('\r', ' ').trim();
        snippet = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        int colon = snippet.indexOf(':');
        if (colon < 0) {
            return "";
        }
        String tail = snippet.substring(colon + 1);
        int cut = tail.length();
        for (String marker : new String[]{"=", ";", "{", "}"}) {
            int idx = tail.indexOf(marker);
            if (idx >= 0) {
                cut = Math.min(cut, idx);
            }
        }
        return tail.substring(0, cut)
            .replaceAll("\\s+", " ")
            .trim();
    }

    static List<String> typeScriptModifiers(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b(public|private|protected|static|abstract|readonly|declare|override|export)\\b").matcher(node.textSnippet());
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result.stream().distinct().toList();
    }

    static String typeScriptAccessibility(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("\\b(public|private|protected)\\b").matcher(node.textSnippet());
        return matcher.find() ? matcher.group(1) : "";
    }

    static boolean typeScriptOptional(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return false;
        }
        String name = declarationName(node);
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
        return node != null && Set.of(
            "method_definition", "method_signature", "abstract_method_signature"
        ).contains(node.type());
    }

    static boolean isTypeScriptPropertyLikeDeclaration(SyntaxNode node) {
        return node != null && Set.of(
            "public_field_definition", "property_signature", "abstract_property_signature", "field_definition"
        ).contains(node.type());
    }

    static String typeScriptMethodReturnType(SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return "";
        }
        String snippet = node.textSnippet().replace('\n', ' ').replace('\r', ' ').trim();
        snippet = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        int closeParen = snippet.indexOf(')');
        if (closeParen < 0) {
            return "";
        }
        String tail = snippet.substring(closeParen + 1).trim();
        if (!tail.startsWith(":")) {
            return "";
        }
        tail = tail.substring(1).trim();
        int cut = tail.length();
        for (String marker : new String[]{"{", ";", "="}) {
            int idx = tail.indexOf(marker);
            if (idx >= 0) {
                cut = Math.min(cut, idx);
            }
        }
        return tail.substring(0, cut).replaceAll("\\s+", " ").trim();
    }

    static List<String> typeScriptMethodParameterDeclaredTypes(SyntaxNode node) {
        String params = parameterSnippet(node);
        if (params == null || params.isBlank() || "()".equals(params.strip())) {
            return List.of();
        }
        String inner = params.strip();
        if (inner.startsWith("(")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        if (inner.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
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
        String snippet = parameterSnippet.replace('\n', ' ').replace('\r', ' ').trim();
        snippet = snippet.replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ");
        int colon = snippet.indexOf(':');
        if (colon < 0) {
            return "";
        }
        String tail = snippet.substring(colon + 1).trim();
        int cut = tail.length();
        for (String marker : new String[]{"=", ","}) {
            int idx = tail.indexOf(marker);
            if (idx >= 0) {
                cut = Math.min(cut, idx);
            }
        }
        return tail.substring(0, cut).replaceAll("\\s+", " ").trim();
    }

    static boolean containsDescendantType(SyntaxNode node, String type) {
        return firstDescendantByType(node, Set.of(type)).isPresent();
    }

    private static void visit(SyntaxNode node, java.util.function.Consumer<SyntaxNode> consumer) {
        consumer.accept(node);
        for (SyntaxNode child : node.children()) {
            visit(child, consumer);
        }
    }
}
