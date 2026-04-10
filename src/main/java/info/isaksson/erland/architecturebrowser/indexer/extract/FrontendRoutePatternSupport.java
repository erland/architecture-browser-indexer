package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FrontendRoutePatternSupport {
    private FrontendRoutePatternSupport() {
    }

    static String firstGroup(Pattern pattern, String text) {
        if (pattern == null || text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    static LinkedHashSet<String> typeIdentifiers(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        Matcher matcher = Pattern.compile("([A-Z][A-Za-z0-9_]*)").matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    static void addIfPresent(LinkedHashSet<String> targets, String value) {
        if (value != null && !value.isBlank()) {
            targets.add(value);
        }
    }
}
