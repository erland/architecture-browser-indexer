package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class TypeScriptResolvedTypeDependencySupport {
    private TypeScriptResolvedTypeDependencySupport() {
    }

    static void addResolvedTypeRelationship(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact sourceType,
        String referencedType,
        EntityKind fallbackTargetKind,
        RelationshipKind relationshipKind,
        String relationshipPrefix,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        ResolvedTypeScriptType resolved = resolveTypeReference(accumulator, referencedType, fallbackTargetKind, relativePath, line, declaredTypes);
        if (resolved == null || sourceType.id().equals(resolved.entityId())) {
            return;
        }
        Map<String, Object> relationshipMetadata = enrichResolvedTargetMetadata(dependencyMetadata(relationshipPrefix, "hierarchy"), resolved);
        accumulator.addRelationship(ExtractionSupport.typedRelationship(
            relationshipKind,
            relationshipPrefix,
            sourceType.id(),
            resolved.entityId(),
            resolved.label(),
            ref,
            "typescript",
            relationshipMetadata
        ));
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            sourceType.id(),
            resolved.entityId(),
            resolved.label(),
            ref,
            "typescript",
            relationshipMetadata
        ));
    }

    static void addDeclaredTypeDependencies(
        ExtractionAccumulator accumulator,
        String sourceEntityId,
        List<String> declaredTypeTexts,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, Object> metadata
    ) {
        if (sourceEntityId == null || declaredTypeTexts == null || declaredTypeTexts.isEmpty()) {
            return;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String declaredTypeText : declaredTypeTexts) {
            for (String referencedType : TypeScriptDeclaredTypeParsingSupport.extractReferencedTypes(declaredTypeText)) {
                ResolvedTypeScriptType resolved = resolveTypeReference(accumulator, referencedType, EntityKind.CLASS, relativePath, line, declaredTypes);
                if (resolved == null || sourceEntityId.equals(resolved.entityId()) || !seen.add(resolved.label())) {
                    continue;
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    sourceEntityId,
                    resolved.entityId(),
                    resolved.label(),
                    ref,
                    "typescript",
                    enrichResolvedTargetMetadata(metadata, resolved)
                ));
            }
        }
    }

    static Map<String, Object> dependencyMetadata(String dependencySource, String dependencyCategory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dependencySource", dependencySource);
        metadata.put("dependencyCategory", dependencyCategory);
        return Map.copyOf(metadata);
    }

    private static ResolvedTypeScriptType resolveTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        int line,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String normalized = TypeScriptDeclaredTypeParsingSupport.normalizeTypeReference(referencedType);
        if (normalized.isBlank()) {
            return null;
        }
        ExtractedEntityFact declared = declaredTypes.get(normalized);
        if (declared != null) {
            String declaredQualifiedName = String.valueOf(declared.metadata().getOrDefault("qualifiedName", declared.name()));
            return new ResolvedTypeScriptType(declared.id(), declaredQualifiedName, declared.kind(), "observed-source-type", "internal");
        }
        boolean internalCandidate = TypeScriptDeclaredTypeParsingSupport.isInternalTypeReference(normalized);
        var inferred = internalCandidate
            ? ExtractionSupport.inferredTypeEntity(
                "typescript",
                fallbackTargetKind,
                normalized,
                relativePath,
                line,
                Map.of(
                    "resolvedFrom", normalized,
                    "resolution", "inferred-internal",
                    "external", false,
                    "inferredInternal", true,
                    "targetClassification", "inferred-internal-type"
                )
            )
            : ExtractionSupport.inferredTypeEntity(
                "typescript",
                fallbackTargetKind,
                normalized,
                relativePath,
                line,
                Map.of(
                    "resolvedFrom", normalized,
                    "resolution", "unresolved-or-external",
                    "targetClassification", "external-package-target"
                )
            );
        accumulator.addEntity(inferred);
        return new ResolvedTypeScriptType(
            inferred.id(),
            normalized,
            inferred.kind(),
            internalCandidate ? "inferred-internal-type" : "external-package-target",
            internalCandidate ? "internal" : "external"
        );
    }

    private static Map<String, Object> enrichResolvedTargetMetadata(Map<String, Object> metadata, ResolvedTypeScriptType resolved) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        if (resolved != null) {
            merged.put("targetClassification", resolved.targetClassification());
            merged.put("dependencyTargetBoundary", resolved.targetBoundary());
        }
        return Map.copyOf(merged);
    }

    private record ResolvedTypeScriptType(
        String entityId,
        String label,
        EntityKind kind,
        String targetClassification,
        String targetBoundary
    ) {
    }
}
