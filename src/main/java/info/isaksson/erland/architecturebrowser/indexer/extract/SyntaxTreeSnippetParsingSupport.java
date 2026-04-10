package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SyntaxTreeSnippetParsingSupport {
    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile("([A-Za-z_][\\w.]*\\*?)");
    private static final Pattern ANNOTATION_NAME_PATTERN = Pattern.compile("@([A-Za-z_][\\w.]*)");

    private SyntaxTreeSnippetParsingSupport() {
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

    static String parameterSnippet(SyntaxNode node) {
        String extracted = SyntaxTreeTraversalSupport.firstDescendantByType(node, java.util.Set.of("formal_parameters", "parameters"))
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

    static List<String> splitTopLevelCommaSeparated(String value) {
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
}
