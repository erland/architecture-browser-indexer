package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class UnavailableSourceParser implements SourceParser {
    private final ParseLanguage language;
    private final TreeSitterLanguageAvailability availability;

    UnavailableSourceParser(ParseLanguage language, TreeSitterLanguageAvailability availability) {
        this.language = language;
        this.availability = availability;
    }

    @Override
    public ParseLanguage language() {
        return language;
    }

    @Override
    public SourceParseResult parse(SourceParseRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>(availability.metadata());
        metadata.put("runtimeAvailable", metadata.getOrDefault("runtimeAvailable", availability.available()));
        metadata.put("language", language.inventoryKey());
        return new SourceParseResult(
            request,
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(new ParseIssue(
                "parse.backend.unavailable",
                availability.detail(),
                null,
                null,
                false
            )),
            metadata
        );
    }
}
