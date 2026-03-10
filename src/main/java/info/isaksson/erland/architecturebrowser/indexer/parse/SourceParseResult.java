package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.List;
import java.util.Map;

public record SourceParseResult(
    SourceParseRequest request,
    ParseStatus status,
    SyntaxTree syntaxTree,
    List<ParseIssue> issues,
    Map<String, Object> metadata
) {
    public SourceParseResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean successful() {
        return status == ParseStatus.SUCCESS && syntaxTree != null;
    }
}
