package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptTypeRelationshipSupport {
    private TypeScriptTypeRelationshipSupport() {
    }

    static void addTypeRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        if (typeNode == null || typeEntity == null) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "typescript", "kind", typeNode.type()));
        for (String parentType : extractExtendedTypes(typeNode)) {
            EntityKind targetKind = typeEntity.kind() == EntityKind.INTERFACE ? EntityKind.INTERFACE : EntityKind.CLASS;
            addResolvedTypeRelationship(accumulator, typeEntity, parentType, targetKind, RelationshipKind.EXTENDS, "extends", relativePath, line, ref, declaredTypes);
        }
        for (String implementedType : extractImplementedTypes(typeNode)) {
            addResolvedTypeRelationship(accumulator, typeEntity, implementedType, EntityKind.INTERFACE, RelationshipKind.IMPLEMENTS, "implements", relativePath, line, ref, declaredTypes);
        }
    }

    static void addPropertyTypeDependencies(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        ExtractedEntityFact propertyEntity,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String declaredType = String.valueOf(propertyEntity.metadata().getOrDefault("declaredType", ""));
        if (declaredType.isBlank()) {
            return;
        }
        addDeclaredTypeDependencies(
            accumulator,
            ownerEntity.id(),
            List.of(declaredType),
            relativePath,
            line,
            ref,
            declaredTypes,
            dependencyMetadata("field", "composition")
        );
    }

    @SuppressWarnings("unchecked")
    static void addMethodTypeDependencies(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        ExtractedEntityFact methodEntity,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String returnType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (!returnType.isBlank()) {
            addDeclaredTypeDependencies(
                accumulator,
                ownerEntity.id(),
                List.of(returnType),
                relativePath,
                line,
                ref,
                declaredTypes,
                dependencyMetadata("returnType", "api")
            );
        }
        List<String> parameterTypes = (List<String>) methodEntity.metadata().getOrDefault("parameterTypes", List.of());
        if (!parameterTypes.isEmpty()) {
            addDeclaredTypeDependencies(
                accumulator,
                ownerEntity.id(),
                parameterTypes,
                relativePath,
                line,
                ref,
                declaredTypes,
                dependencyMetadata("constructor".equals(methodEntity.name()) ? "constructorParameter" : "parameterType", "api")
            );
        }
    }

    private static void addResolvedTypeRelationship(
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

    private static ResolvedTypeScriptType resolveTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        int line,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String normalized = normalizeTypeReference(referencedType);
        if (normalized.isBlank()) {
            return null;
        }
        ExtractedEntityFact declared = declaredTypes.get(normalized);
        if (declared != null) {
            String declaredQualifiedName = String.valueOf(declared.metadata().getOrDefault("qualifiedName", declared.name()));
            return new ResolvedTypeScriptType(declared.id(), declaredQualifiedName, declared.kind(), "observed-source-type", "internal");
        }
        boolean internalCandidate = isInternalTypeReference(normalized);
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

    private static void addDeclaredTypeDependencies(
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
            for (String referencedType : extractReferencedTypes(declaredTypeText)) {
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

    private static List<String> extractReferencedTypes(String declaredTypeText) {
        if (declaredTypeText == null || declaredTypeText.isBlank()) {
            return List.of();
        }
        String normalized = declaredTypeText
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\\bextends\\b", " ")
            .replaceAll("\\bkeyof\\b", " ")
            .replaceAll("\\breadonly\\b", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .replace("...", " ")
            .replace("|", " ")
            .replace("&", " ");
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!isTypeScriptPrimitiveOrKeyword(candidate)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isTypeScriptPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "string", "number", "boolean", "void", "null", "undefined", "unknown", "never", "any",
            "object", "symbol", "bigint", "true", "false", "this", "super"
        ).contains(candidate);
    }

    private static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        return extractClauseTypes(typeNode, "extends_clause");
    }

    private static List<String> extractImplementedTypes(SyntaxNode typeNode) {
        return extractClauseTypes(typeNode, "implements_clause");
    }

    private static List<String> extractClauseTypes(SyntaxNode typeNode, String clauseType) {
        if (typeNode == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (SyntaxNode clauseNode : typeNode.children()) {
            if (!clauseType.equals(clauseNode.type())) {
                continue;
            }
            for (SyntaxNode candidate : SyntaxTreeExtractionSupport.descendantsByType(clauseNode, Set.of("type_identifier", "nested_type_identifier", "predefined_type", "identifier"))) {
                String normalized = normalizeTypeReference(candidate.textSnippet());
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean isInternalTypeReference(String normalized) {
        return normalized != null && (normalized.contains(".") || normalized.contains("/") || normalized.contains("#"));
    }

    private static String normalizeTypeReference(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
            .replaceAll("<[^>]+>", " ")
            .replace("?", " ")
            .replace("[]", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*)").matcher(normalized);
        return matcher.find() ? matcher.group(1) : "";
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

    private static Map<String, Object> dependencyMetadata(String dependencySource, String dependencyCategory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dependencySource", dependencySource);
        metadata.put("dependencyCategory", dependencyCategory);
        return Map.copyOf(metadata);
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
