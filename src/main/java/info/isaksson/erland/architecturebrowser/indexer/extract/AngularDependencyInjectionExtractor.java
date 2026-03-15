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

final class AngularDependencyInjectionExtractor {
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("constructor\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern INJECT_TOKEN_PATTERN = Pattern.compile("@Inject\\s*\\(\\s*([A-Za-z_$][\\w.$]*)\\s*\\)");
    private static final Pattern TYPE_ANNOTATION_PATTERN = Pattern.compile(":\\s*([A-Za-z_$][\\w.$]*)");
    private static final Set<String> IGNORED_TYPES = Set.of(
        "string", "number", "boolean", "object", "unknown", "any", "void", "null", "undefined"
    );

    private AngularDependencyInjectionExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        String sourceText,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || relativePath == null || namedEntities == null || namedEntities.isEmpty()) {
            return;
        }
        if (!looksLikeAngularDiSource(relativePath, sourceText, namedEntities)) {
            return;
        }

        for (ExtractedEntityFact entity : new LinkedHashSet<>(namedEntities.values())) {
            if (entity == null) {
                continue;
            }
            if (isAngularProviderOwner(entity)) {
                extractProviderRelationships(accumulator, relativePath, entity, namedEntities);
            }
            if (isAngularInjectableConsumer(entity)) {
                extractConstructorInjectionRelationships(accumulator, relativePath, entity, namedEntities);
            }
        }
    }

    private static boolean looksLikeAngularDiSource(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerPath = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
        String lowerSource = sourceText == null ? "" : sourceText.toLowerCase(Locale.ROOT);
        if (lowerPath.contains("/app/") || lowerPath.contains(".component.") || lowerPath.contains(".service.") || lowerPath.contains(".module.")) {
            return true;
        }
        if (lowerSource.contains("@injectable") || lowerSource.contains("@component") || lowerSource.contains("@ngmodule") || lowerSource.contains("@directive")) {
            return true;
        }
        return namedEntities.values().stream().anyMatch(AngularDependencyInjectionExtractor::isAngularEntity);
    }

    private static boolean isAngularEntity(ExtractedEntityFact entity) {
        return entity != null && "angular".equals(entity.metadata().get("framework"));
    }

    private static boolean isAngularProviderOwner(ExtractedEntityFact entity) {
        return isAngularEntity(entity) && entity.metadata().get("angularProviders") instanceof List<?>;
    }

    private static boolean isAngularInjectableConsumer(ExtractedEntityFact entity) {
        return isAngularEntity(entity)
            && entity.kind() == EntityKind.CLASS
            && !Objects.toString(primaryRef(entity, "").snippet(), "").isBlank();
    }

    private static void extractProviderRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact ownerEntity,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        for (String providerEntry : listMetadata(ownerEntity, "angularProviders")) {
            String raw = providerEntry == null ? "" : providerEntry.strip();
            if (raw.isBlank()) {
                continue;
            }
            if (raw.startsWith("{") && raw.endsWith("}")) {
                extractConfiguredProviderRelationships(accumulator, relativePath, ownerEntity, raw, namedEntities);
            } else {
                ExtractedEntityFact providedEntity = resolveAngularDiTarget(accumulator, relativePath, raw, namedEntities, inferProviderTargetKind(raw));
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    providedEntity.id(),
                    ownerEntity.id(),
                    raw,
                    primaryRef(ownerEntity, relativePath),
                    "typescript",
                    diRelationshipMetadata("providedBy", providedEntity.name(), true, "provider-owner")
                ));
            }
        }
    }

    private static void extractConfiguredProviderRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact ownerEntity,
        String providerEntry,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        Map<String, String> fields = topLevelObjectFields(providerEntry);
        String tokenRaw = normalizeAngularReference(fields.get("provide"));
        if (tokenRaw.isBlank()) {
            return;
        }
        ExtractedEntityFact tokenEntity = resolveAngularDiTarget(accumulator, relativePath, tokenRaw, namedEntities, inferTokenKind(tokenRaw));
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            tokenEntity.id(),
            ownerEntity.id(),
            tokenRaw,
            primaryRef(ownerEntity, relativePath),
            "typescript",
            diRelationshipMetadata("providedBy", tokenRaw, true, "provider-owner")
        ));

        String useClass = normalizeAngularReference(fields.get("useClass"));
        if (!useClass.isBlank()) {
            ExtractedEntityFact implementationEntity = resolveAngularDiTarget(accumulator, relativePath, useClass, namedEntities, EntityKind.CLASS);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                implementationEntity.id(),
                useClass,
                primaryRef(ownerEntity, relativePath),
                "typescript",
                diRelationshipMetadata("resolvesTo", tokenRaw, namedEntities.containsKey(useClass), "provider-resolution")
            ));
        }

        String useExisting = normalizeAngularReference(fields.get("useExisting"));
        if (!useExisting.isBlank()) {
            ExtractedEntityFact implementationEntity = resolveAngularDiTarget(accumulator, relativePath, useExisting, namedEntities, EntityKind.CLASS);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                implementationEntity.id(),
                useExisting,
                primaryRef(ownerEntity, relativePath),
                "typescript",
                diRelationshipMetadata("resolvesTo", tokenRaw, namedEntities.containsKey(useExisting), "provider-resolution")
            ));
        }

        String useFactory = normalizeAngularReference(fields.get("useFactory"));
        if (!useFactory.isBlank()) {
            ExtractedEntityFact factoryEntity = resolveAngularDiTarget(accumulator, relativePath, useFactory, namedEntities, EntityKind.FUNCTION);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                factoryEntity.id(),
                useFactory,
                primaryRef(ownerEntity, relativePath),
                "typescript",
                diRelationshipMetadata("resolvesTo", tokenRaw, namedEntities.containsKey(useFactory), "provider-resolution")
            ));
        }

        if (fields.containsKey("useValue")) {
            String rawValue = Objects.toString(fields.get("useValue"), "").strip();
            String valueTargetName = tokenRaw + ".value";
            ExtractedEntityFact valueEntity = ExtractionSupport.inferredTypeEntity(
                "angular",
                EntityKind.MODULE,
                valueTargetName,
                relativePath,
                refLine(ownerEntity),
                Map.of(
                    "framework", "angular",
                    "angularDiValue", true,
                    "entityRole", "provider-value",
                    "targetClassification", "angular-di-value",
                    "external", false,
                    "inferredInternal", true,
                    "providerValueSnippet", rawValue
                )
            );
            accumulator.addEntity(valueEntity);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                valueEntity.id(),
                rawValue,
                primaryRef(ownerEntity, relativePath),
                "typescript",
                diRelationshipMetadata("resolvesTo", tokenRaw, false, "provider-resolution")
            ));
        }
    }

    private static void extractConstructorInjectionRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact ownerEntity,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        String snippet = Objects.toString(primaryRef(ownerEntity, relativePath).snippet(), "");
        if (snippet.isBlank()) {
            return;
        }
        for (String constructorParameters : extractConstructorParameterBlocks(snippet)) {
            for (String parameter : splitTopLevel(constructorParameters, ',')) {
                InjectionReference reference = parseInjectionReference(parameter);
                if (reference == null || reference.targetName().isBlank()) {
                    continue;
                }
                ExtractedEntityFact injectedEntity = resolveAngularDiTarget(
                    accumulator,
                    relativePath,
                    reference.targetName(),
                    namedEntities,
                    reference.kind()
                );
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    ownerEntity.id(),
                    injectedEntity.id(),
                    reference.label(),
                    primaryRef(ownerEntity, relativePath),
                    "typescript",
                    injectionRelationshipMetadata(reference, true)
                ));
            }
        }
    }


    private static List<String> extractConstructorParameterBlocks(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int searchFrom = 0;
        while (searchFrom < snippet.length()) {
            int constructorIndex = snippet.indexOf("constructor", searchFrom);
            if (constructorIndex < 0) {
                break;
            }
            int parenStart = snippet.indexOf('(', constructorIndex);
            if (parenStart < 0) {
                break;
            }
            int parenEnd = findMatchingParen(snippet, parenStart);
            if (parenEnd < 0) {
                break;
            }
            String parameters = snippet.substring(parenStart + 1, parenEnd).trim();
            if (!parameters.isBlank()) {
                result.add(parameters);
            }
            searchFrom = parenEnd + 1;
        }
        return List.copyOf(result);
    }

    private static int findMatchingParen(String value, int startIndex) {
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean escaped = false;
        for (int i = startIndex; i < value.length(); i++) {
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
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    private static InjectionReference parseInjectionReference(String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return null;
        }
        String raw = parameter.strip();
        Matcher injectMatcher = INJECT_TOKEN_PATTERN.matcher(raw);
        if (injectMatcher.find()) {
            String tokenName = normalizeAngularReference(injectMatcher.group(1));
            if (!tokenName.isBlank()) {
                return new InjectionReference(tokenName, tokenName, inferTokenKind(tokenName), "token");
            }
        }
        Matcher typeMatcher = TYPE_ANNOTATION_PATTERN.matcher(raw);
        if (typeMatcher.find()) {
            String typeName = normalizeAngularReference(typeMatcher.group(1));
            if (!typeName.isBlank() && !IGNORED_TYPES.contains(typeName.toLowerCase(Locale.ROOT))) {
                return new InjectionReference(typeName, typeName, inferProviderTargetKind(typeName), "type");
            }
        }
        return null;
    }

    private static ExtractedEntityFact resolveAngularDiTarget(
        ExtractionAccumulator accumulator,
        String relativePath,
        String rawReference,
        Map<String, ExtractedEntityFact> namedEntities,
        EntityKind fallbackKind
    ) {
        String normalized = normalizeAngularReference(rawReference);
        ExtractedEntityFact existing = namedEntities.get(normalized);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("targetClassification", "angular-di-target");
        metadata.put("resolution", "inferred-angular-di-target");
        metadata.put("external", false);
        metadata.put("inferredInternal", true);
        if (fallbackKind == EntityKind.MODULE) {
            metadata.put("angularToken", true);
            metadata.put("entityRole", "token");
            metadata.put("uiProfile", "angular-di-token");
        }
        ExtractedEntityFact inferred = ExtractionSupport.inferredTypeEntity(
            "angular",
            fallbackKind,
            normalized,
            relativePath,
            1,
            Map.copyOf(metadata)
        );
        accumulator.addEntity(inferred);
        return inferred;
    }

    private static Map<String, Object> injectionRelationshipMetadata(InjectionReference reference, boolean resolved) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("frameworkRelationship", "injects");
        metadata.put("dependencySource", "angular:injects");
        metadata.put("dependencyCategory", "di");
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("targetClassification", reference.kind() == EntityKind.MODULE ? "angular-di-token" : "angular-di-target");
        metadata.put("resolvedFromAngularDiExtraction", resolved);
        metadata.put("injectionKind", "constructor");
        metadata.put("injectionReferenceKind", reference.referenceKind());
        metadata.put("angularReference", reference.targetName());
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> diRelationshipMetadata(String frameworkRelationship, String angularReference, boolean resolved, String dependencyCategory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", "angular:" + frameworkRelationship);
        metadata.put("dependencyCategory", dependencyCategory);
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("targetClassification", "angular-di-target");
        metadata.put("resolvedFromAngularDiExtraction", resolved);
        metadata.put("angularReference", angularReference);
        return Map.copyOf(metadata);
    }

    @SuppressWarnings("unchecked")
    private static List<String> listMetadata(ExtractedEntityFact entity, String key) {
        Object value = entity.metadata().get(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) {
                    result.add(s);
                }
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    private static EntityKind inferProviderTargetKind(String raw) {
        if (raw == null) {
            return EntityKind.CLASS;
        }
        String trimmed = raw.strip();
        if (trimmed.endsWith("()")) {
            return EntityKind.FUNCTION;
        }
        return EntityKind.CLASS;
    }

    private static EntityKind inferTokenKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityKind.MODULE;
        }
        return raw.equals(raw.toUpperCase(Locale.ROOT)) || raw.endsWith("TOKEN") || raw.contains("CONFIG")
            ? EntityKind.MODULE
            : EntityKind.CLASS;
    }

    private static String normalizeAngularReference(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.strip();
        normalized = normalized.replaceAll("^\\[|\\]$", "").trim();
        normalized = normalized.replaceAll("<[^>]+>", " ");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static SourceReference primaryRef(ExtractedEntityFact entity, String relativePath) {
        return entity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, 1, entity.name(), Map.of("language", "typescript", "framework", "angular"))
            : entity.sourceRefs().getFirst();
    }

    private static int refLine(ExtractedEntityFact entity) {
        return entity.sourceRefs().isEmpty() || entity.sourceRefs().getFirst().startLine() == null
            ? 1
            : entity.sourceRefs().getFirst().startLine();
    }

    private static Map<String, String> topLevelObjectFields(String objectLiteral) {
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
                    String token = current.toString().trim();
                    if (!token.isBlank()) {
                        result.add(token);
                    }
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        String token = current.toString().trim();
        if (!token.isBlank()) {
            result.add(token);
        }
        return List.copyOf(result);
    }

    private record InjectionReference(String targetName, String label, EntityKind kind, String referenceKind) {
    }
}
