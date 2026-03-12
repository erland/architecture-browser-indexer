package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.LinkedHashMap;
import java.util.Map;

public record TreeSitterLanguageAvailability(
    ParseLanguage language,
    boolean available,
    String detail,
    Map<String, Object> metadata
) {
    public TreeSitterLanguageAvailability {
        metadata = sanitizeMetadata(metadata);
    }

    public static TreeSitterLanguageAvailability available(ParseLanguage language, String detail, Map<String, Object> metadata) {
        return new TreeSitterLanguageAvailability(language, true, detail, metadata);
    }

    public static TreeSitterLanguageAvailability unavailable(ParseLanguage language, String detail, Map<String, Object> metadata) {
        return new TreeSitterLanguageAvailability(language, false, detail, metadata);
    }

    private static Map<String, Object> sanitizeMetadata(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            sanitized.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return Map.copyOf(sanitized);
    }
}
