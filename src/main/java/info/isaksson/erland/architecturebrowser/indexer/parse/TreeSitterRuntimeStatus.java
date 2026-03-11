package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.Map;

public record TreeSitterRuntimeStatus(
    boolean available,
    String detail,
    Map<String, Object> metadata
) {
    public TreeSitterRuntimeStatus {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static TreeSitterRuntimeStatus available(String detail) {
        return new TreeSitterRuntimeStatus(true, detail, Map.of());
    }

    public static TreeSitterRuntimeStatus available(String detail, Map<String, Object> metadata) {
        return new TreeSitterRuntimeStatus(true, detail, metadata);
    }

    public static TreeSitterRuntimeStatus unavailable(String detail) {
        return new TreeSitterRuntimeStatus(false, detail, Map.of());
    }

    public static TreeSitterRuntimeStatus unavailable(String detail, Map<String, Object> metadata) {
        return new TreeSitterRuntimeStatus(false, detail, metadata);
    }

    public TreeSitterLanguageAvailability languageAvailable(ParseLanguage language, String detail, Map<String, Object> metadata) {
        return TreeSitterLanguageAvailability.available(language, detail, mergedMetadata(metadata));
    }

    public TreeSitterLanguageAvailability languageUnavailable(ParseLanguage language, String detail, Map<String, Object> metadata) {
        return TreeSitterLanguageAvailability.unavailable(language, detail, mergedMetadata(metadata));
    }

    private Map<String, Object> mergedMetadata(Map<String, Object> extraMetadata) {
        if (extraMetadata == null || extraMetadata.isEmpty()) {
            return metadata;
        }
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>(metadata);
        merged.putAll(extraMetadata);
        return Map.copyOf(merged);
    }
}
