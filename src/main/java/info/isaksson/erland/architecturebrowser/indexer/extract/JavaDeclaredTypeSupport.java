package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.ArrayList;
import java.util.List;

final class JavaDeclaredTypeSupport {

    private JavaDeclaredTypeSupport() {}

    @SuppressWarnings("unchecked")
    static List<String> metadataStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    static List<String> extractParameterNames(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank() || "()".equals(parameterSnippet.strip())) {
            return List.of();
        }
        String inner = parameterSnippet.strip();
        if (inner.startsWith("(")) inner = inner.substring(1);
        if (inner.endsWith(")")) inner = inner.substring(0, inner.length() - 1);
        if (inner.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
            String name = extractParameterName(part.strip());
            if (!name.isBlank()) {
                result.add(name);
            }
        }
        return List.copyOf(result);
    }


    private static List<String> splitTopLevelCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<' || ch == '(' || ch == '[') {
                depth++;
            } else if (ch == '>' || ch == ')' || ch == ']') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ',' && depth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        return List.copyOf(result);
    }

    private static String extractParameterName(String snippet) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([A-Za-z_$][\\w$]*)\\s*$").matcher(snippet);
        return matcher.find() ? matcher.group(1) : "";
    }

}
