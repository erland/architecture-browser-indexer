package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AngularProviderRelationshipEmitter {
    private AngularProviderRelationshipEmitter() {
    }

    static void extractProviderRelationships(
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
                ExtractedEntityFact providedEntity = AngularDependencyInjectionTargetResolver.resolveAngularDiTarget(
                    accumulator,
                    relativePath,
                    raw,
                    namedEntities,
                    AngularDependencyInjectionReferenceSupport.inferProviderTargetKind(raw)
                );
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
        ExtractedEntityFact tokenEntity = AngularDependencyInjectionTargetResolver.resolveAngularDiTarget(
            accumulator,
            relativePath,
            tokenRaw,
            namedEntities,
            AngularDependencyInjectionReferenceSupport.inferTokenKind(tokenRaw)
        );
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            tokenEntity.id(),
            ownerEntity.id(),
            tokenRaw,
            AngularSourceSupport.primaryRef(ownerEntity, relativePath),
            "typescript",
            diRelationshipMetadata("providedBy", tokenRaw, true, "provider-owner")
        ));

        emitConfiguredResolution(accumulator, relativePath, ownerEntity, namedEntities, tokenEntity, tokenRaw, fields, "useClass", EntityKind.CLASS);
        emitConfiguredResolution(accumulator, relativePath, ownerEntity, namedEntities, tokenEntity, tokenRaw, fields, "useExisting", EntityKind.CLASS);
        emitConfiguredResolution(accumulator, relativePath, ownerEntity, namedEntities, tokenEntity, tokenRaw, fields, "useFactory", EntityKind.FUNCTION);

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

    private static void emitConfiguredResolution(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact ownerEntity,
        Map<String, ExtractedEntityFact> namedEntities,
        ExtractedEntityFact tokenEntity,
        String tokenRaw,
        Map<String, String> fields,
        String fieldName,
        EntityKind fallbackKind
    ) {
        String rawReference = fieldReference(fields, null, fieldName);
        if (rawReference.isBlank()) {
            return;
        }
        ExtractedEntityFact implementationEntity = AngularDependencyInjectionTargetResolver.resolveAngularDiTarget(
            accumulator,
            relativePath,
            rawReference,
            namedEntities,
            fallbackKind
        );
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            tokenEntity.id(),
            implementationEntity.id(),
            rawReference,
            AngularSourceSupport.primaryRef(ownerEntity, relativePath),
            "typescript",
            diRelationshipMetadata("resolvesTo", tokenRaw, namedEntities.containsKey(rawReference), "provider-resolution")
        ));
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
}
