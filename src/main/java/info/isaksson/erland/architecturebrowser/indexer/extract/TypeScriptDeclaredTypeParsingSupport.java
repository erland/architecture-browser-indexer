package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptDeclaredTypeParsingSupport {
    private TypeScriptDeclaredTypeParsingSupport() {
    }

    static List<String> extractReferencedTypes(String declaredTypeText) {
        if (declaredTypeText == null || declaredTypeText.isBlank()) {
            return List.of();
        }
        String normalized = declaredTypeText
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\bextends\b", " ")
            .replaceAll("\bkeyof\b", " ")
            .replaceAll("\breadonly\b", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .replace("...", " ")
            .replace("|", " ")
            .replace("&", " ");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!isTypeScriptPrimitiveOrKeyword(candidate)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        return extractClauseTypes(typeNode, "extends_clause");
    }

    static List<String> extractImplementedTypes(SyntaxNode typeNode) {
        return extractClauseTypes(typeNode, "implements_clause");
    }

    static String normalizeTypeReference(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
            .replaceAll("<[^>]+>", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
    }

    static boolean isInternalTypeReference(String normalized) {
        return normalized != null && (normalized.contains(".") || normalized.contains("/") || normalized.contains("#"));
    }

    private static List<String> extractClauseTypes(SyntaxNode typeNode, String clauseType) {
        if (typeNode == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (SyntaxNode clauseNode : typeNode.children()) {
            if (!clauseType.equals(clauseNode.type())) {
                continue;
            }
            for (SyntaxNode candidate : SyntaxTreeExtractionSupport.descendantsByType(clauseNode, Set.of("type_identifier", "nested_type_identifier", "predefined_type", "identifier"))) {
                String normalized = normalizeTypeReference(candidate.textSnippet());
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean isTypeScriptPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "string", "number", "boolean", "void", "null", "undefined", "unknown", "never", "any",
            "object", "symbol", "bigint", "true", "false", "this", "super"
        ).contains(candidate);
    }
}
