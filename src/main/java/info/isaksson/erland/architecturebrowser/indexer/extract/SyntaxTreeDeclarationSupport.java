package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Set;

final class SyntaxTreeDeclarationSupport {
    private static final Set<String> DECLARATION_IDENTIFIER_TYPES = Set.of(
        "identifier",
        "type_identifier",
        "property_identifier"
    );

    private SyntaxTreeDeclarationSupport() {
    }

    static int oneBasedLine(SyntaxNode node) {
        return SyntaxTreeNavigationSupport.oneBasedLine(node);
    }

    static String declarationName(SyntaxNode node) {
        return SyntaxTreeTraversalSupport.firstDescendantByType(node, DECLARATION_IDENTIFIER_TYPES)
            .map(SyntaxNode::textSnippet)
            .orElse(null);
    }
}
