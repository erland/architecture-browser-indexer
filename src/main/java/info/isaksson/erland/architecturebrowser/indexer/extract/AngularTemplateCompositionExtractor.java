package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AngularTemplateCompositionExtractor {
    private static final Pattern TAG_PATTERN = Pattern.compile("<\\s*([a-z][a-z0-9-]*)\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(?:\\*|\\[)?([A-Za-z_][A-Za-z0-9_-]*)(?:\\])?(?:\\s*=)?");
    private static final Pattern PIPE_PATTERN = Pattern.compile("\\|\\s*([A-Za-z_][A-Za-z0-9_]*)");

    private AngularTemplateCompositionExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || namedEntities == null || namedEntities.isEmpty()) {
            return;
        }
        Map<String, ExtractedEntityFact> componentSelectors = new LinkedHashMap<>();
        Map<String, ExtractedEntityFact> directiveSelectors = new LinkedHashMap<>();
        Map<String, ExtractedEntityFact> pipeNames = new LinkedHashMap<>();
        for (ExtractedEntityFact entity : uniqueEntities(namedEntities)) {
            if (entity == null || entity.kind() != EntityKind.CLASS) {
                continue;
            }
            String framework = Objects.toString(entity.metadata().get("framework"), "");
            if (!"angular".equals(framework)) {
                continue;
            }
            String angularKind = Objects.toString(entity.metadata().get("angularKind"), "");
            if ("component".equals(angularKind)) {
                for (String selector : normalizedSelectorValues(entity.metadata().get("angularSelector"))) {
                    componentSelectors.putIfAbsent(selector, entity);
                }
            } else if ("directive".equals(angularKind)) {
                for (String selector : normalizedSelectorValues(entity.metadata().get("angularSelector"))) {
                    directiveSelectors.putIfAbsent(selector, entity);
                }
            } else if ("pipe".equals(angularKind)) {
                String pipeName = normalizePipeName(entity.metadata().get("angularPipeName"));
                if (!pipeName.isBlank()) {
                    pipeNames.putIfAbsent(pipeName, entity);
                }
            }
        }

        for (ExtractedEntityFact entity : uniqueEntities(namedEntities)) {
            if (!isAngularComponent(entity)) {
                continue;
            }
            SourceReference ref = primaryRef(entity, relativePath);
            String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
            String template = inlineTemplate(entity, snippet);
            if (template.isBlank()) {
                continue;
            }
            LinkedHashSet<String> componentTargets = componentTags(template);
            componentTargets.removeAll(normalizedSelectorValues(entity.metadata().get("angularSelector")));
            for (String selector : componentTargets) {
                ExtractedEntityFact target = componentSelectors.get(selector);
                boolean resolved = target != null;
                if (target == null) {
                    target = inferredAngularTemplateTarget(selector, EntityKind.UI_MODULE, "component", "selector");
                    accumulator.addEntity(target);
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    target.id(),
                    target.name(),
                    templateRef(relativePath, ref.startLine(), template),
                    "typescript",
                    relationshipMetadata("templateRenders", "angular:template-renders", resolved, "component-selector")
                ));
            }

            for (String attribute : attributeNames(template)) {
                ExtractedEntityFact target = directiveSelectors.get(attribute);
                boolean resolved = target != null;
                if (target == null) {
                    target = inferredAngularTemplateTarget(attribute, EntityKind.UI_MODULE, "directive", "selector");
                    accumulator.addEntity(target);
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    target.id(),
                    target.name(),
                    templateRef(relativePath, ref.startLine(), template),
                    "typescript",
                    relationshipMetadata("usesDirective", "angular:template-uses-directive", resolved, "directive-selector")
                ));
            }

            for (String pipeName : pipeNames(template)) {
                ExtractedEntityFact target = pipeNames.get(pipeName);
                boolean resolved = target != null;
                if (target == null) {
                    target = inferredAngularTemplateTarget(pipeName, EntityKind.FUNCTION, "pipe", "name");
                    accumulator.addEntity(target);
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    target.id(),
                    target.name(),
                    templateRef(relativePath, ref.startLine(), template),
                    "typescript",
                    relationshipMetadata("usesPipe", "angular:template-uses-pipe", resolved, "pipe-name")
                ));
            }
        }
    }

    private static Set<ExtractedEntityFact> uniqueEntities(Map<String, ExtractedEntityFact> namedEntities) {
        return new LinkedHashSet<>(namedEntities.values());
    }

    private static boolean isAngularComponent(ExtractedEntityFact entity) {
        if (entity == null || entity.kind() != EntityKind.CLASS) {
            return false;
        }
        return "angular".equals(entity.metadata().get("framework"))
            && "component".equals(entity.metadata().get("angularKind"));
    }

    private static SourceReference primaryRef(ExtractedEntityFact entity, String relativePath) {
        return entity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, 1, entity.name(), Map.of("language", "typescript", "framework", "angular"))
            : entity.sourceRefs().getFirst();
    }

    private static SourceReference templateRef(String relativePath, Integer line, String template) {
        int safeLine = line == null ? 1 : line;
        return ExtractionSupport.sourceRef(relativePath, safeLine, template, Map.of("language", "typescript", "framework", "angular", "kind", "template"));
    }

    private static ExtractedEntityFact inferredAngularTemplateTarget(String name, EntityKind kind, String angularKind, String selectorKind) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("angularKind", angularKind);
        metadata.put("inferredInternal", true);
        metadata.put("inferredFrom", "angular-template");
        metadata.put("selectorKind", selectorKind);
        metadata.put("entityRole", angularKind);
        metadata.put("uiProfile", "angular-" + angularKind);
        return ExtractionSupport.inferredTypeEntity("typescript", kind, name, null, 1, Map.copyOf(metadata));
    }

    private static Map<String, Object> relationshipMetadata(String frameworkRelationship, String dependencySource, boolean resolved, String targetClassification) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", dependencySource);
        metadata.put("dependencyCategory", "framework");
        metadata.put("targetClassification", targetClassification);
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("resolvedFromAngularTemplateExtraction", resolved);
        return Map.copyOf(metadata);
    }

    private static String inlineTemplate(ExtractedEntityFact entity, String declarationSnippet) {
        if (entity != null) {
            String fromMetadata = Objects.toString(entity.metadata().get("angularInlineTemplate"), "").trim();
            if (!fromMetadata.isBlank()) {
                return fromMetadata;
            }
        }
        String componentPayload = decoratorPayload(declarationSnippet, "@Component");
        if (componentPayload.isBlank()) {
            return "";
        }
        Map<String, String> fields = topLevelObjectFields(componentPayload);
        return stringLiteralContent(fields.get("template"));
    }

    private static String decoratorPayload(String snippet, String decoratorName) {
        if (snippet == null || snippet.isBlank()) {
            return "";
        }
        int decoratorIndex = snippet.indexOf(decoratorName);
        if (decoratorIndex < 0) {
            return "";
        }
        int parenStart = snippet.indexOf('(', decoratorIndex + decoratorName.length());
        if (parenStart < 0) {
            return "";
        }
        int parenEnd = findMatchingParen(snippet, parenStart);
        if (parenEnd < 0) {
            return "";
        }
        String body = snippet.substring(parenStart + 1, parenEnd).trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            return body;
        }
        int objectStart = body.indexOf('{');
        if (objectStart < 0) {
            return "";
        }
        int objectEnd = findMatchingBrace(body, objectStart);
        if (objectEnd < 0) {
            return "";
        }
        return body.substring(objectStart, objectEnd + 1);
    }

    private static int findMatchingParen(String value, int openIndex) {
        return findMatching(value, openIndex, '(', ')');
    }

    private static int findMatchingBrace(String value, int openIndex) {
        return findMatching(value, openIndex, '{', '}');
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

    private static Map<String, String> topLevelObjectFields(String objectLiteral) {
        if (objectLiteral == null || objectLiteral.isBlank()) {
            return Map.of();
        }
        String body = objectLiteral.trim();
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

    private static String stringLiteralContent(String raw) {
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

    private static LinkedHashSet<String> componentTags(String template) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (template == null || template.isBlank()) {
            return result;
        }
        Matcher matcher = TAG_PATTERN.matcher(template);
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (tag != null && tag.contains("-")) {
                result.add(tag.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private static LinkedHashSet<String> attributeNames(String template) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (template == null || template.isBlank()) {
            return result;
        }
        Matcher tagMatcher = TAG_PATTERN.matcher(template);
        while (tagMatcher.find()) {
            String attrs = Objects.toString(tagMatcher.group(2), "");
            Matcher attrMatcher = ATTRIBUTE_PATTERN.matcher(attrs);
            while (attrMatcher.find()) {
                String name = attrMatcher.group(1);
                if (name == null || name.isBlank()) {
                    continue;
                }
                String normalized = name.toLowerCase(Locale.ROOT);
                if (normalized.startsWith("ng-") || normalized.startsWith("aria-") || normalized.startsWith("data-")
                    || "class".equals(normalized) || "id".equals(normalized) || "style".equals(normalized)) {
                    continue;
                }
                result.add(normalized);
            }
        }
        return result;
    }

    private static LinkedHashSet<String> pipeNames(String template) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (template == null || template.isBlank()) {
            return result;
        }
        Matcher matcher = PIPE_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name != null && !name.isBlank()) {
                result.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private static List<String> normalizedSelectorValues(Object selectorMetadata) {
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

    private static String normalizePipeName(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }
}
