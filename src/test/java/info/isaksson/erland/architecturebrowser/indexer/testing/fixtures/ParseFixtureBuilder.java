package info.isaksson.erland.architecturebrowser.indexer.testing.fixtures;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ParseFixtureBuilder {
    private ParseFixtureBuilder() {}

    public static SourceParseResult parsedFile(String relativePath, ParseLanguage language, String source, SyntaxNode root) {
        return new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, language, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(language, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
    }

    public static ParseBatchResult successfulBatch(List<SourceParseResult> results) {
        Map<ParseLanguage, Long> perLanguage = results.stream()
            .collect(Collectors.groupingBy(result -> result.request().language(), Collectors.counting()));
        Map<ParseStatus, Long> perStatus = results.stream()
            .collect(Collectors.groupingBy(SourceParseResult::status, Collectors.counting()));
        return new ParseBatchResult(results, toIntegerMap(perLanguage), toIntegerMap(perStatus));
    }

    private static <K> Map<K, Integer> toIntegerMap(Map<K, Long> source) {
        return source.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
    }
}
