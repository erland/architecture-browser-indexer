package info.isaksson.erland.architecturebrowser.indexer.parse;

public record SyntaxTree(
    ParseLanguage language,
    String parserBackend,
    SyntaxNode root,
    boolean hasErrors,
    int nodeCount
) {
}
