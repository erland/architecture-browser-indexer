package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

final class SyntaxTreeTraversalSupport {
    private SyntaxTreeTraversalSupport() {
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

    static boolean containsDescendantType(SyntaxNode node, String type) {
        return firstDescendantByType(node, Set.of(type)).isPresent();
    }

    private static void visit(SyntaxNode node, Consumer<SyntaxNode> consumer) {
        consumer.accept(node);
        for (SyntaxNode child : node.children()) {
            visit(child, consumer);
        }
    }
}
