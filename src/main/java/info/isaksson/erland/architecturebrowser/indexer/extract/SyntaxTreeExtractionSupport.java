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
        return node == null ? 1 : node.startLine() + 1;
    }

    static String declarationName(SyntaxNode node) {
        return firstDescendantByType(node, Set.of("identifier", "type_identifier", "property_identifier"))
            .map(SyntaxNode::textSnippet)
            .orElse(null);
    }

    static String parameterSnippet(SyntaxNode node) {
        return firstDescendantByType(node, Set.of("formal_parameters", "parameters"))
            .map(SyntaxNode::textSnippet)
            .orElse("");
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
