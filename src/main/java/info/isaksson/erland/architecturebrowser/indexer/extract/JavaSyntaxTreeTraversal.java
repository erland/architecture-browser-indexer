package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

final class JavaSyntaxTreeTraversal {

    void traverse(
        SyntaxNode node,
        JavaTraversalOwnership ownership,
        JavaSyntaxTreeTraversalHandler handler
    ) {
        if (node == null) {
            return;
        }
        JavaTraversalOwnership nextOwnership = handler.handleNode(node, ownership);
        for (SyntaxNode child : node.children()) {
            traverse(child, nextOwnership, handler);
        }
    }

    @FunctionalInterface
    interface JavaSyntaxTreeTraversalHandler {
        JavaTraversalOwnership handleNode(SyntaxNode node, JavaTraversalOwnership ownership);
    }

    record JavaTraversalOwnership(
        String owningTypeEntityId,
        String owningQualifiedName,
        String owningTypeSnippet
    ) {}
}
