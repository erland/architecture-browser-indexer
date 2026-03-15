package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class AngularDecoratorMetadataExtractor {
    private static final Set<String> SUPPORTED_DECORATORS = Set.of("Component", "Directive", "Pipe", "NgModule", "Injectable");

    private AngularDecoratorMetadataExtractor() {
    }

    static Map<String, Object> extract(SyntaxNode declarationNode) {
        if (declarationNode == null) {
            return Map.of();
        }
        List<SyntaxNode> decorators = SyntaxTreeExtractionSupport.descendantsByType(declarationNode, Set.of("decorator"));
        if (decorators.isEmpty()) {
            return Map.of();
        }
        for (SyntaxNode decoratorNode : decorators) {
            String snippet = decoratorNode.textSnippet();
            Optional<String> decoratorName = decoratorName(snippet);
            if (decoratorName.isEmpty() || !SUPPORTED_DECORATORS.contains(decoratorName.get())) {
                continue;
            }
            return switch (decoratorName.get()) {
                case "Component" -> componentMetadata(snippet);
                case "Directive" -> directiveMetadata(snippet);
                case "Pipe" -> pipeMetadata(snippet);
                case "NgModule" -> ngModuleMetadata(snippet);
                case "Injectable" -> injectableMetadata(snippet);
                default -> Map.of();
            };
        }
        return Map.of();
    }

    private static Map<String, Object> componentMetadata(String snippet) {
        Map<String, String> payload = topLevelObjectFields(snippet);
        Map<String, Object> metadata = angularMetadataBase("Component", "component");
        putIfNotBlank(metadata, "angularSelector", stringLiteral(payload.get("selector")));
        putIfNotBlank(metadata, "angularTemplateUrl", stringLiteral(payload.get("templateUrl")));
        String inlineTemplate = stringLiteral(payload.get("template"));
        if (payload.containsKey("template")) {
            metadata.put("angularHasInlineTemplate", !payload.get("template").isBlank());
        }
        putIfNotBlank(metadata, "angularInlineTemplate", inlineTemplate);
        putIfPresent(metadata, "angularStyleUrls", arrayOrSingleton(payload.get("styleUrls")));
        putIfPresent(metadata, "angularStandalone", booleanLiteral(payload.get("standalone")));
        putIfPresent(metadata, "angularImports", arrayOrSingleton(payload.get("imports")));
        putIfPresent(metadata, "angularProviders", arrayOrSingleton(payload.get("providers")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> directiveMetadata(String snippet) {
        Map<String, String> payload = topLevelObjectFields(snippet);
        Map<String, Object> metadata = angularMetadataBase("Directive", "directive");
        putIfNotBlank(metadata, "angularSelector", stringLiteral(payload.get("selector")));
        putIfPresent(metadata, "angularStandalone", booleanLiteral(payload.get("standalone")));
        putIfPresent(metadata, "angularProviders", arrayOrSingleton(payload.get("providers")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> pipeMetadata(String snippet) {
        Map<String, String> payload = topLevelObjectFields(snippet);
        Map<String, Object> metadata = angularMetadataBase("Pipe", "pipe");
        putIfNotBlank(metadata, "angularPipeName", stringLiteral(payload.get("name")));
        putIfPresent(metadata, "angularStandalone", booleanLiteral(payload.get("standalone")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> ngModuleMetadata(String snippet) {
        Map<String, String> payload = topLevelObjectFields(snippet);
        Map<String, Object> metadata = angularMetadataBase("NgModule", "module");
        putIfPresent(metadata, "angularImports", arrayOrSingleton(payload.get("imports")));
        putIfPresent(metadata, "angularDeclarations", arrayOrSingleton(payload.get("declarations")));
        putIfPresent(metadata, "angularExports", arrayOrSingleton(payload.get("exports")));
        putIfPresent(metadata, "angularProviders", arrayOrSingleton(payload.get("providers")));
        putIfPresent(metadata, "angularBootstrap", arrayOrSingleton(payload.get("bootstrap")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> injectableMetadata(String snippet) {
        Map<String, String> payload = topLevelObjectFields(snippet);
        Map<String, Object> metadata = angularMetadataBase("Injectable", "injectable");
        putIfNotBlank(metadata, "angularProvidedIn", stringLiteral(payload.get("providedIn")));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> angularMetadataBase(String decorator, String kind) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("angularDecorator", decorator);
        metadata.put("angularKind", kind);
        return metadata;
    }

    private static Optional<String> decoratorName(String snippet) {
        List<String> annotations = SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(snippet);
        if (annotations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(annotations.getFirst());
    }

    private static Map<String, String> topLevelObjectFields(String decoratorSnippet) {
        String objectLiteral = firstObjectLiteral(decoratorSnippet);
        if (objectLiteral.isBlank()) {
            return Map.of();
        }
        String body = objectLiteral.substring(1, objectLiteral.length() - 1).trim();
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

    private static String firstObjectLiteral(String snippet) {
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

    private static List<String> splitTopLevel(String value, char delimiter) {
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
                inSingle = !inSingle;
                current.append(ch);
                continue;
            }
            if (!inSingle && !inBacktick && ch == '"') {
                inDouble = !inDouble;
                current.append(ch);
                continue;
            }
            if (!inSingle && !inDouble && ch == '`') {
                inBacktick = !inBacktick;
                current.append(ch);
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
                if (ch == delimiter && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                    String part = current.toString().trim();
                    if (!part.isEmpty()) {
                        result.add(part);
                    }
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        return List.copyOf(result);
    }

    private static List<String> arrayOrSingleton(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        String value = rawValue.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            String body = value.substring(1, value.length() - 1).trim();
            if (body.isBlank()) {
                return List.of();
            }
            LinkedHashSet<String> items = new LinkedHashSet<>();
            for (String part : splitTopLevel(body, ',')) {
                String normalized = normalizeValue(part);
                if (!normalized.isBlank()) {
                    items.add(normalized);
                }
            }
            return List.copyOf(items);
        }
        String normalized = normalizeValue(value);
        return normalized.isBlank() ? List.of() : List.of(normalized);
    }

    private static String normalizeValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        String asLiteral = stringLiteral(rawValue);
        return asLiteral.isBlank() ? rawValue.trim() : asLiteral;
    }

    private static Optional<Boolean> booleanLiteral(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawValue.trim();
        if ("true".equals(normalized)) {
            return Optional.of(true);
        }
        if ("false".equals(normalized)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    private static String stringLiteral(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        String value = rawValue.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"') || (first == '`' && last == '`')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return "";
    }

    private static void putIfNotBlank(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Optional<Boolean> value) {
        value.ifPresent(booleanValue -> metadata.put(key, booleanValue));
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            metadata.put(key, values);
        }
    }
}
