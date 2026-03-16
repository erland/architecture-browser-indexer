package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AngularLiteralSupport {
    private AngularLiteralSupport() {
    }

    public static Map<String, String> topLevelObjectFields(String objectLiteral) {
        String body = objectLiteral == null ? "" : objectLiteral.strip();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1).trim();
        }
        if (body.isBlank()) {
            return Map.of();
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (String entry : splitTopLevel(body, ',')) {
            int colon = firstTopLevelColon(entry);
            if (colon < 0) {
                continue;
            }
            String key = entry.substring(0, colon).trim();
            String value = entry.substring(colon + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                fields.put(key, value);
            }
        }
        return Map.copyOf(fields);
    }

    public static String firstObjectLiteral(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return "";
        }
        int start = -1;
        int braceDepth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean escaped = false;
        for (int i = 0; i < snippet.length(); i++) {
            char ch = snippet.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && (inSingle || inDouble || inBacktick)) {
                escaped = true;
                continue;
            }
            if (!inDouble && !inBacktick && ch == '\'') {
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && !inBacktick && ch == '"') {
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && ch == '`') {
                inBacktick = !inBacktick;
                continue;
            }
            if (inSingle || inDouble || inBacktick) {
                continue;
            }
            if (ch == '{') {
                if (braceDepth == 0) {
                    start = i;
                }
                braceDepth++;
            } else if (ch == '}') {
                braceDepth--;
                if (braceDepth == 0 && start >= 0) {
                    return snippet.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    public static int findMatchingParen(String value, int openIndex) {
        return findMatching(value, openIndex, '(', ')');
    }

    public static int findMatchingBrace(String value, int openIndex) {
        return findMatching(value, openIndex, '{', '}');
    }

    public static String stringLiteralContent(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' || first == '"' || first == '`') && last == first) {
                return value.substring(1, value.length() - 1);
            }
        }
        return "";
    }

    public static List<String> splitTopLevel(String value, char delimiter) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\' && (inSingle || inDouble || inBacktick)) {
                current.append(ch);
                escaped = true;
                continue;
            }
            if (!inDouble && !inBacktick && ch == '\'') {
                current.append(ch);
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && !inBacktick && ch == '"') {
                current.append(ch);
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && ch == '`') {
                current.append(ch);
                inBacktick = !inBacktick;
                continue;
            }
            if (!inSingle && !inDouble && !inBacktick) {
                switch (ch) {
                    case '{' -> braceDepth++;
                    case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                    case '[' -> bracketDepth++;
                    case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                    case '(' -> parenDepth++;
                    case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                    default -> {
                    }
                }
            }
            if (ch == delimiter && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0 && !inSingle && !inDouble && !inBacktick) {
                String token = current.toString().trim();
                if (!token.isBlank()) {
                    result.add(token);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String token = current.toString().trim();
        if (!token.isBlank()) {
            result.add(token);
        }
        return List.copyOf(result);
    }

    private static int firstTopLevelColon(String value) {
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && (inSingle || inDouble || inBacktick)) {
                escaped = true;
                continue;
            }
            if (!inDouble && !inBacktick && ch == '\'') {
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && !inBacktick && ch == '"') {
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && ch == '`') {
                inBacktick = !inBacktick;
                continue;
            }
            if (inSingle || inDouble || inBacktick) {
                continue;
            }
            switch (ch) {
                case '{' -> braceDepth++;
                case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                case '(' -> parenDepth++;
                case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                case ':' -> {
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        return i;
                    }
                }
                default -> {
                }
            }
        }
        return -1;
    }

    private static int findMatching(String value, int openIndex, char open, char close) {
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean escaped = false;
        for (int i = openIndex; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && (inSingle || inDouble || inBacktick)) {
                escaped = true;
                continue;
            }
            if (!inDouble && !inBacktick && ch == '\'') {
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && !inBacktick && ch == '"') {
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && ch == '`') {
                inBacktick = !inBacktick;
                continue;
            }
            if (inSingle || inDouble || inBacktick) {
                continue;
            }
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
