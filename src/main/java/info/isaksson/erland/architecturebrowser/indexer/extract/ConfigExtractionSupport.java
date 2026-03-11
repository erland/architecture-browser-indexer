package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ConfigExtractionSupport {
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("\"([^\"]+)\"|'([^']+)'");
    private static final Pattern KEY_VALUE_COLON_PATTERN = Pattern.compile("^\\s*['\"]?([A-Za-z0-9_.\\-/]+)['\"]?\\s*:\\s*(.+?)\\s*$", Pattern.DOTALL);
    private static final Pattern KEY_VALUE_EQUALS_PATTERN = Pattern.compile("^\\s*([A-Za-z0-9_.\\-]+)\\s*=\\s*(.+?)\\s*$", Pattern.DOTALL);
    private static final Pattern XML_ELEMENT_PATTERN = Pattern.compile("^\\s*<([A-Za-z0-9_.:-]+)>(.*?)</\\1>\\s*$", Pattern.DOTALL);
    private static final Pattern XML_ATTRIBUTE_PATTERN = Pattern.compile("([A-Za-z0-9_.:-]+)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern JDBC_PATTERN = Pattern.compile("(jdbc:[A-Za-z0-9:;_./?=&%-]+)");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s'\"<>)]+)");
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("\\b([A-Za-z0-9_.-]+:\\d{2,5})\\b");

    private ConfigExtractionSupport() {
    }

    static Optional<ConfigEntryCandidate> parseCandidate(String relativePath, String languageKey, SyntaxNode node) {
        if (node == null || node.textSnippet() == null || node.textSnippet().isBlank()) {
            return Optional.empty();
        }
        String snippet = sanitize(node.textSnippet());
        return switch (languageKey) {
            case "json", "yaml" -> parseMapLikeCandidate(relativePath, languageKey, node, snippet);
            case "properties" -> parsePropertiesCandidate(relativePath, node, snippet);
            case "xml" -> parseXmlCandidate(relativePath, node, snippet);
            default -> Optional.empty();
        };
    }

    static List<String> dependencyTargets(String key, String value) {
        List<String> targets = new ArrayList<>();
        if (value != null) {
            addMatches(targets, JDBC_PATTERN, value);
            addMatches(targets, URL_PATTERN, value);
            addMatches(targets, HOST_PORT_PATTERN, value);
        }
        String normalizedKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
        if (normalizedKey.contains("datasource") && value != null && !value.isBlank()) {
            targets.add(value.trim());
        }
        if (normalizedKey.contains("bootstrap-servers") && value != null && !value.isBlank()) {
            targets.add(value.trim());
        }
        return targets.stream().map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
    }

    static boolean datastoreLike(String key, String value) {
        String normalized = (key == null ? "" : key.toLowerCase(Locale.ROOT)) + " " + (value == null ? "" : value.toLowerCase(Locale.ROOT));
        return normalized.contains("datasource")
            || normalized.contains("jdbc:")
            || normalized.contains("postgres")
            || normalized.contains("mysql")
            || normalized.contains("oracle")
            || normalized.contains("mssql")
            || normalized.contains("sqlite");
    }

    static String compactValue(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = sanitize(raw);
        if (cleaned.length() > 120) {
            return cleaned.substring(0, 120);
        }
        return cleaned;
    }

    static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.strip().replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " ");
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Optional<ConfigEntryCandidate> parseMapLikeCandidate(String relativePath, String languageKey, SyntaxNode node, String snippet) {
        Matcher matcher = KEY_VALUE_COLON_PATTERN.matcher(snippet);
        if (matcher.matches()) {
            return Optional.of(new ConfigEntryCandidate(
                matcher.group(1),
                compactValue(stripTrailingDecorators(matcher.group(2))),
                relativePath,
                languageKey,
                node.startLine() + 1,
                snippet
            ));
        }
        return Optional.empty();
    }

    private static Optional<ConfigEntryCandidate> parsePropertiesCandidate(String relativePath, SyntaxNode node, String snippet) {
        Matcher matcher = KEY_VALUE_EQUALS_PATTERN.matcher(snippet);
        if (matcher.matches()) {
            return Optional.of(new ConfigEntryCandidate(
                matcher.group(1),
                compactValue(matcher.group(2)),
                relativePath,
                "properties",
                node.startLine() + 1,
                snippet
            ));
        }
        return Optional.empty();
    }

    private static Optional<ConfigEntryCandidate> parseXmlCandidate(String relativePath, SyntaxNode node, String snippet) {
        Matcher elementMatcher = XML_ELEMENT_PATTERN.matcher(snippet);
        if (elementMatcher.matches()) {
            return Optional.of(new ConfigEntryCandidate(
                elementMatcher.group(1),
                compactValue(elementMatcher.group(2)),
                relativePath,
                "xml",
                node.startLine() + 1,
                snippet
            ));
        }
        Matcher attributeMatcher = XML_ATTRIBUTE_PATTERN.matcher(snippet);
        if (attributeMatcher.find()) {
            return Optional.of(new ConfigEntryCandidate(
                attributeMatcher.group(1),
                compactValue(attributeMatcher.group(2)),
                relativePath,
                "xml",
                node.startLine() + 1,
                snippet
            ));
        }
        return Optional.empty();
    }

    private static String stripTrailingDecorators(String raw) {
        String cleaned = raw == null ? "" : raw.strip();
        Matcher quoted = QUOTED_VALUE_PATTERN.matcher(cleaned);
        if (quoted.find()) {
            String first = quoted.group(1) != null ? quoted.group(1) : quoted.group(2);
            return first;
        }
        if (cleaned.startsWith("{") || cleaned.startsWith("[") || cleaned.startsWith("<")) {
            return cleaned;
        }
        return cleaned.replaceFirst("[,}]\\s*$", "");
    }

    private static void addMatches(List<String> target, Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            target.add(matcher.group(1));
        }
    }

    record ConfigEntryCandidate(
        String key,
        String value,
        String relativePath,
        String languageKey,
        int line,
        String snippet
    ) {
    }
}
