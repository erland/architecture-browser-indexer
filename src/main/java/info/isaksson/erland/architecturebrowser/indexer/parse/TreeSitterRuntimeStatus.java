package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.LinkedHashMap;
import java.util.Map;

public record TreeSitterRuntimeStatus(
    boolean available,
    String detail,
    Map<String, Object> metadata
) {
    public TreeSitterRuntimeStatus {
        metadata = sanitizeMetadata(metadata);
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
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(metadata);
        merged.putAll(extraMetadata);
        return sanitizeMetadata(merged);
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
            sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }
        return Map.copyOf(sanitized);
    }

    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> nestedMap) {
            LinkedHashMap<String, Object> sanitizedNested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                if (nestedEntry.getKey() == null) {
                    continue;
                }
                sanitizedNested.put(String.valueOf(nestedEntry.getKey()), sanitizeValue(nestedEntry.getValue()));
            }
            return Map.copyOf(sanitizedNested);
        }
        return value;
    }
}
