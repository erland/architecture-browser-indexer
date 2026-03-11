package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.Map;

public record TreeSitterLanguageAvailability(
    ParseLanguage language,
    boolean available,
    String detail,
    Map<String, Object> metadata
) {
    public TreeSitterLanguageAvailability {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static TreeSitterLanguageAvailability available(ParseLanguage language, String detail, Map<String, Object> metadata) {
        return new TreeSitterLanguageAvailability(language, true, detail, metadata);
    }

    public static TreeSitterLanguageAvailability unavailable(ParseLanguage language, String detail, Map<String, Object> metadata) {
        return new TreeSitterLanguageAvailability(language, false, detail, metadata);
    }
}
