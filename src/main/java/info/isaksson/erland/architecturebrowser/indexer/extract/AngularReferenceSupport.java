package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AngularReferenceSupport {
    private static final Pattern ANGULAR_REFERENCE_PATTERN = Pattern.compile("([A-Za-z_$][\\w.$]*)");

    private AngularReferenceSupport() {
    }

    public static String normalizeReference(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.strip();
        normalized = normalized.replaceAll("^\\[|\\]$", "").trim();

        int openParen = normalized.lastIndexOf('(');
        int closeParen = normalized.endsWith(")") ? normalized.length() - 1 : -1;
        if (openParen >= 0 && closeParen > openParen) {
            String callArgument = normalizeReference(normalized.substring(openParen + 1, closeParen));
            if (!callArgument.isBlank()) {
                return callArgument;
            }
        }

        normalized = normalized.replaceAll("<[^>]+>", " ");
        Matcher matcher = ANGULAR_REFERENCE_PATTERN.matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
    }

    public static List<String> normalizedSelectorValues(Object selectorMetadata) {
        if (selectorMetadata == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (selectorMetadata instanceof List<?> list) {
            for (Object item : list) {
                values.addAll(normalizedSelectorValues(item));
            }
            return List.copyOf(values);
        }
        String raw = String.valueOf(selectorMetadata).trim();
        if (raw.isBlank()) {
            return List.of();
        }
        for (String candidate : raw.split(",")) {
            String value = candidate.trim();
            if (value.isBlank()) {
                continue;
            }
            if (value.startsWith("[") && value.endsWith("]")) {
                values.add(value.substring(1, value.length() - 1).trim().toLowerCase(Locale.ROOT));
                continue;
            }
            int bracketStart = value.indexOf('[');
            int bracketEnd = value.indexOf(']');
            if (bracketStart >= 0 && bracketEnd > bracketStart) {
                values.add(value.substring(bracketStart + 1, bracketEnd).trim().toLowerCase(Locale.ROOT));
                continue;
            }
            if (value.startsWith(".")) {
                values.add(value.substring(1).trim().toLowerCase(Locale.ROOT));
                continue;
            }
            values.add(value.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(values);
    }

    public static String normalizePipeName(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }
}
