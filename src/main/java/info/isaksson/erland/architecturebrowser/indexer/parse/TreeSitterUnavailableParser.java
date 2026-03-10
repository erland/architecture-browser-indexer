package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.List;
import java.util.Map;

final class TreeSitterUnavailableParser implements SourceParser {
    private final ParseLanguage language;
    private final TreeSitterRuntimeStatus runtimeStatus;

    TreeSitterUnavailableParser(ParseLanguage language, TreeSitterRuntimeStatus runtimeStatus) {
        this.language = language;
        this.runtimeStatus = runtimeStatus;
    }

    @Override
    public ParseLanguage language() {
        return language;
    }

    @Override
    public SourceParseResult parse(SourceParseRequest request) {
        return new SourceParseResult(
            request,
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(new ParseIssue(
                "parse.backend.unavailable",
                runtimeStatus.detail(),
                null,
                null,
                false
            )),
            Map.of("runtimeAvailable", runtimeStatus.available(), "language", language.inventoryKey())
        );
    }
}
