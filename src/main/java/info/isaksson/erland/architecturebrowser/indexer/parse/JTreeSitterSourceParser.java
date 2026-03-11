package info.isaksson.erland.architecturebrowser.indexer.parse;

import io.github.treesitter.jtreesitter.InputEncoding;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Point;
import io.github.treesitter.jtreesitter.Tree;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JTreeSitterSourceParser implements SourceParser {
    private final ParseLanguage language;
    private final TreeSitterLanguageDescriptor descriptor;
    private final JTreeSitterLanguageLoader.LoadedLanguage loadedLanguage;

    JTreeSitterSourceParser(
        ParseLanguage language,
        TreeSitterLanguageDescriptor descriptor,
        JTreeSitterLanguageLoader.LoadedLanguage loadedLanguage
    ) {
        this.language = language;
        this.descriptor = descriptor;
        this.loadedLanguage = loadedLanguage;
    }

    @Override
    public ParseLanguage language() {
        return language;
    }

    @Override
    public SourceParseResult parse(SourceParseRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("parserBackend", "tree-sitter-jtreesitter");
        metadata.put("grammarName", descriptor.grammarName());
        metadata.put("sharedLibrary", loadedLanguage.libraryFileName());
        metadata.put("lookupTarget", loadedLanguage.lookupTarget());
        metadata.put("libraryResolutionMode", loadedLanguage.resolutionMode());
        metadata.put("relativePath", request.relativePath());
        loadedLanguage.languageName().ifPresent(name -> metadata.put("languageName", name));
        metadata.put("languageAbiVersion", loadedLanguage.language().getAbiVersion());

        try (Parser parser = new Parser(loadedLanguage.language())) {
            Tree tree = parser.parse(request.sourceText(), InputEncoding.UTF_8)
                .orElseThrow(() -> new IllegalStateException("Parser returned no syntax tree"));
            try (tree) {
                Node rootNode = tree.getRootNode();
                SyntaxNode root = toSyntaxNode(rootNode, 0);
                boolean hasErrors = rootNode.hasError();
                return new SourceParseResult(
                    request,
                    hasErrors ? ParseStatus.PARSE_ERROR : ParseStatus.SUCCESS,
                    new SyntaxTree(language, "tree-sitter-jtreesitter", root, hasErrors, root.nodeCount()),
                    hasErrors
                        ? List.of(new ParseIssue("parse.syntax.error", "Tree-sitter reported syntax errors", null, null, false))
                        : List.of(),
                    metadata
                );
            }
        } catch (Throwable t) {
            return new SourceParseResult(
                request,
                ParseStatus.BACKEND_UNAVAILABLE,
                null,
                List.of(new ParseIssue("parse.backend.failure", t.getMessage(), null, null, true)),
                metadata
            );
        }
    }

    private SyntaxNode toSyntaxNode(Node node, int depth) {
        List<Node> children = node.getChildren();
        List<SyntaxNode> mappedChildren = children.stream()
            .map(child -> toSyntaxNode(child, depth + 1))
            .toList();

        String textSnippet = null;
        try {
            textSnippet = node.getText();
        } catch (RuntimeException ignored) {
            // best effort only
        }
        if (textSnippet != null && textSnippet.length() > 200) {
            textSnippet = textSnippet.substring(0, 200);
        }

        Point startPoint = node.getStartPoint();
        Point endPoint = node.getEndPoint();
        return new SyntaxNode(
            node.getType(),
            node.isNamed(),
            node.getStartByte(),
            node.getEndByte(),
            startPoint.row(),
            startPoint.column(),
            endPoint.row(),
            endPoint.column(),
            node.isError(),
            node.isMissing(),
            textSnippet,
            mappedChildren
        );
    }
}
