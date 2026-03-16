package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

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
            && !Objects.toString(AngularSourceSupport.primaryRef(entity, "").snippet(), "").isBlank();
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
                    AngularSourceSupport.primaryRef(ownerEntity, relativePath),
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
        Map<String, String> fields = AngularLiteralSupport.topLevelObjectFields(providerEntry);
        String tokenRaw = fieldReference(fields, providerEntry, "provide");
        if (tokenRaw.isBlank()) {
            return;
        }
        ExtractedEntityFact tokenEntity = resolveAngularDiTarget(accumulator, relativePath, tokenRaw, namedEntities, inferTokenKind(tokenRaw));
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            tokenEntity.id(),
            ownerEntity.id(),
            tokenRaw,
            AngularSourceSupport.primaryRef(ownerEntity, relativePath),
            "typescript",
            diRelationshipMetadata("providedBy", tokenRaw, true, "provider-owner")
        ));

        String useClass = fieldReference(fields, providerEntry, "useClass");
        if (!useClass.isBlank()) {
            ExtractedEntityFact implementationEntity = resolveAngularDiTarget(accumulator, relativePath, useClass, namedEntities, EntityKind.CLASS);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                implementationEntity.id(),
                useClass,
                AngularSourceSupport.primaryRef(ownerEntity, relativePath),
                "typescript",
                diRelationshipMetadata("resolvesTo", tokenRaw, namedEntities.containsKey(useClass), "provider-resolution")
            ));
        }

        String useExisting = fieldReference(fields, providerEntry, "useExisting");
        if (!useExisting.isBlank()) {
            ExtractedEntityFact implementationEntity = resolveAngularDiTarget(accumulator, relativePath, useExisting, namedEntities, EntityKind.CLASS);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                implementationEntity.id(),
                useExisting,
                AngularSourceSupport.primaryRef(ownerEntity, relativePath),
                "typescript",
                diRelationshipMetadata("resolvesTo", tokenRaw, namedEntities.containsKey(useExisting), "provider-resolution")
            ));
        }

        String useFactory = fieldReference(fields, providerEntry, "useFactory");
        if (!useFactory.isBlank()) {
            ExtractedEntityFact factoryEntity = resolveAngularDiTarget(accumulator, relativePath, useFactory, namedEntities, EntityKind.FUNCTION);
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                tokenEntity.id(),
                factoryEntity.id(),
                useFactory,
                AngularSourceSupport.primaryRef(ownerEntity, relativePath),
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
                AngularSourceSupport.refLine(ownerEntity),
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
                AngularSourceSupport.primaryRef(ownerEntity, relativePath),
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
        String snippet = Objects.toString(AngularSourceSupport.primaryRef(ownerEntity, relativePath).snippet(), "");
        if (snippet.isBlank()) {
            return;
        }
        for (String constructorParameters : extractConstructorParameterBlocks(snippet)) {
            for (String parameter : AngularLiteralSupport.splitTopLevel(constructorParameters, ',')) {
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
                    AngularSourceSupport.primaryRef(ownerEntity, relativePath),
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
            int parenEnd = AngularLiteralSupport.findMatchingParen(snippet, parenStart);
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

    private static InjectionReference parseInjectionReference(String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return null;
        }
        String raw = parameter.strip();
        Matcher injectMatcher = INJECT_TOKEN_PATTERN.matcher(raw);
        if (injectMatcher.find()) {
            String tokenName = AngularReferenceSupport.normalizeReference(injectMatcher.group(1));
            if (!tokenName.isBlank()) {
                return new InjectionReference(tokenName, tokenName, inferTokenKind(tokenName), "token");
            }
        }
        Matcher typeMatcher = TYPE_ANNOTATION_PATTERN.matcher(raw);
        if (typeMatcher.find()) {
            String typeName = AngularReferenceSupport.normalizeReference(typeMatcher.group(1));
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
        String normalized = AngularReferenceSupport.normalizeReference(rawReference);
        ExtractedEntityFact existing = namedEntities.get(normalized);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("targetClassification", fallbackKind == EntityKind.MODULE ? "angular-di-token" : "angular-di-target");
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

    private static String fieldReference(Map<String, String> fields, String providerEntry, String fieldName) {
        String direct = AngularReferenceSupport.normalizeReference(fields.get(fieldName));
        if (!direct.isBlank() && !fieldName.equals(direct)) {
            return direct;
        }
        if (providerEntry == null || providerEntry.isBlank()) {
            return direct;
        }
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(fieldName) + "\\s*:\\s*([A-Za-z_$][\\w.$]*)").matcher(providerEntry);
        if (matcher.find()) {
            String fallback = AngularReferenceSupport.normalizeReference(matcher.group(1));
            if (!fallback.isBlank()) {
                return fallback;
            }
        }
        return direct;
    }

    private record InjectionReference(String targetName, String label, EntityKind kind, String referenceKind) {
    }
}
