package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AngularFrameworkRelationshipExtractor {
    private AngularFrameworkRelationshipExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || namedEntities == null || namedEntities.isEmpty()) {
            return;
        }
        for (ExtractedEntityFact entity : namedEntities.values()) {
            if (entity == null || !"angular".equals(entity.metadata().get("framework"))) {
                continue;
            }
            String angularKind = Objects.toString(entity.metadata().get("angularKind"), "");
            switch (angularKind) {
                case "module" -> extractModuleRelationships(accumulator, relativePath, entity, namedEntities);
                case "component", "directive" -> extractStandaloneRelationships(accumulator, relativePath, entity, namedEntities);
                case "injectable" -> extractInjectableRelationships(accumulator, relativePath, entity);
                default -> {
                }
            }
        }
    }

    private static void extractModuleRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact moduleEntity,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        SourceReference ref = primaryRef(moduleEntity, relativePath);
        addRelationships(accumulator, relativePath, moduleEntity, listMetadata(moduleEntity, "angularDeclarations"), "declares", namedEntities, ref);
        addRelationships(accumulator, relativePath, moduleEntity, listMetadata(moduleEntity, "angularImports"), "imports", namedEntities, ref);
        addRelationships(accumulator, relativePath, moduleEntity, listMetadata(moduleEntity, "angularExports"), "exports", namedEntities, ref);
        addRelationships(accumulator, relativePath, moduleEntity, listMetadata(moduleEntity, "angularBootstrap"), "bootstraps", namedEntities, ref);
        addRelationships(accumulator, relativePath, moduleEntity, listMetadata(moduleEntity, "angularProviders"), "provides", namedEntities, ref);
    }

    private static void extractStandaloneRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact componentEntity,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (!Boolean.TRUE.equals(componentEntity.metadata().get("angularStandalone"))) {
            return;
        }
        SourceReference ref = primaryRef(componentEntity, relativePath);
        addRelationships(accumulator, relativePath, componentEntity, listMetadata(componentEntity, "angularImports"), "imports", namedEntities, ref);
        addRelationships(accumulator, relativePath, componentEntity, listMetadata(componentEntity, "angularProviders"), "provides", namedEntities, ref);
    }

    private static void extractInjectableRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact serviceEntity
    ) {
        Object providedIn = serviceEntity.metadata().get("angularProvidedIn");
        if (!(providedIn instanceof String scope) || scope.isBlank()) {
            return;
        }
        String normalized = normalizeAngularReference(scope);
        if (normalized.isBlank()) {
            normalized = scope;
        }
        Map<String, Object> metadata = frameworkRelationshipMetadata("providedBy", normalized, true, "application-scope");
        var target = ExtractionSupport.externalDependencyEntity(
            "angular",
            "application:" + normalized,
            relativePath,
            refLine(serviceEntity),
            Map.of(
                "framework", "angular",
                "angularScope", normalized,
                "targetClassification", "angular-application-scope",
                "external", true
            )
        );
        accumulator.addEntity(target);
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            serviceEntity.id(),
            target.id(),
            scope,
            primaryRef(serviceEntity, relativePath),
            "typescript",
            metadata
        ));
    }

    private static String normalizeProviderRelationshipTarget(String frameworkRelationship, String raw) {
        if (!"provides".equals(frameworkRelationship) || raw == null || raw.isBlank()) {
            return normalizeAngularReference(raw);
        }
        Matcher provideMatcher = Pattern.compile("\\bprovide\\s*:\\s*([A-Za-z_$][\\w.$]*)").matcher(raw);
        if (provideMatcher.find()) {
            String token = normalizeAngularReference(provideMatcher.group(1));
            if (!token.isBlank()) {
                return token;
            }
        }
        return normalizeAngularReference(raw);
    }

    private static void addRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact sourceEntity,
        List<String> values,
        String frameworkRelationship,
        Map<String, ExtractedEntityFact> namedEntities,
        SourceReference ref
    ) {
        for (String rawValue : values) {
            String raw = rawValue == null ? "" : rawValue.strip();
            if (raw.isBlank()) {
                continue;
            }
            String normalized = normalizeProviderRelationshipTarget(frameworkRelationship, raw);
            if (normalized.isBlank()) {
                continue;
            }
            ExtractedEntityFact targetEntity = namedEntities.get(normalized);
            boolean resolved = targetEntity != null;
            if (!resolved) {
                EntityKind fallbackKind = inferTargetKind(frameworkRelationship, normalized, raw);
                targetEntity = ExtractionSupport.inferredTypeEntity(
                    "angular",
                    fallbackKind,
                    normalized,
                    relativePath,
                    refLine(sourceEntity),
                    Map.of(
                        "framework", "angular",
                        "targetClassification", "angular-framework-target",
                        "resolution", "inferred-angular-framework-relationship",
                        "external", false,
                        "inferredInternal", true
                    )
                );
                accumulator.addEntity(targetEntity);
            }
            Map<String, Object> metadata = frameworkRelationshipMetadata(frameworkRelationship, normalized, resolved, "framework");
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                sourceEntity.id(),
                targetEntity.id(),
                raw,
                ref,
                "typescript",
                metadata
            ));
        }
    }

    private static EntityKind inferTargetKind(String frameworkRelationship, String normalized, String raw) {
        if ("provides".equals(frameworkRelationship) || raw.endsWith("()")) {
            return raw.endsWith("()") ? EntityKind.FUNCTION : EntityKind.CLASS;
        }
        if (normalized.endsWith("Module") || normalized.contains(".forChild") || normalized.contains(".forRoot")) {
            return EntityKind.MODULE;
        }
        return EntityKind.CLASS;
    }

    private static Map<String, Object> frameworkRelationshipMetadata(String frameworkRelationship, String normalized, boolean resolved, String dependencyCategory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", "angular:" + frameworkRelationship);
        metadata.put("dependencyCategory", dependencyCategory);
        metadata.put("targetClassification", "angular-framework-target");
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("resolvedFromDecoratorPayload", resolved);
        metadata.put("angularReference", normalized);
        return Map.copyOf(metadata);
    }

    @SuppressWarnings("unchecked")
    private static List<String> listMetadata(ExtractedEntityFact entity, String key) {
        Object value = entity.metadata().get(key);
        if (value instanceof List<?> list) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) {
                    result.add(s);
                }
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    private static String normalizeAngularReference(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.strip();
        normalized = normalized.replaceAll("^\\[|\\]$", "").trim();
        normalized = normalized.replaceAll("<[^>]+>", " ");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
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
}
