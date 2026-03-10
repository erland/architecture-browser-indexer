package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.List;

public record SyntaxNode(
    String type,
    boolean named,
    int startByte,
    int endByte,
    int startLine,
    int startColumn,
    int endLine,
    int endColumn,
    boolean error,
    boolean missing,
    String textSnippet,
    List<SyntaxNode> children
) {
    public SyntaxNode {
        children = children == null ? List.of() : List.copyOf(children);
    }

    public int nodeCount() {
        return 1 + children.stream().mapToInt(SyntaxNode::nodeCount).sum();
    }
}
