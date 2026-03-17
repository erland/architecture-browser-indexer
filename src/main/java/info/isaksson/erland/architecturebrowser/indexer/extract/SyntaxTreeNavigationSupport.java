package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SyntaxTreeNavigationSupport {
    private SyntaxTreeNavigationSupport() {
    }

    static List<SyntaxNode> childrenByType(SyntaxNode node, String type) {
        if (node == null) {
            return List.of();
        }
        return node.children().stream().filter(child -> type.equals(child.type())).toList();
    }

    static Optional<SyntaxNode> firstChildByType(SyntaxNode node, String type) {
        if (node == null) {
            return Optional.empty();
        }
        return node.children().stream().filter(child -> type.equals(child.type())).findFirst();
    }

    static List<SyntaxNode> descendantsByType(SyntaxNode node, Set<String> types) {
        if (node == null || types == null || types.isEmpty()) {
            return List.of();
        }
        List<SyntaxNode> matches = new ArrayList<>();
        collectDescendants(node, types, matches);
        return List.copyOf(matches);
    }

    private static void collectDescendants(SyntaxNode node, Set<String> types, List<SyntaxNode> matches) {
        for (SyntaxNode child : node.children()) {
            if (types.contains(child.type())) {
                matches.add(child);
            }
            collectDescendants(child, types, matches);
        }
    }

    static int oneBasedLine(SyntaxNode node) {
        return node == null ? 1 : node.startLine() + 1;
    }
}
