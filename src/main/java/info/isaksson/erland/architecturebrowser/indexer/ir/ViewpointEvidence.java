package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

record ViewpointEvidence(
    List<ArchitectureEntity> entities,
    List<ArchitectureRelationship> relationships,
    Map<String, Object> dependencyViews,
    Map<String, List<String>> entityIdsByRole,
    Set<String> relationshipSemantics,
    boolean hasJavaInterpretationEvidence,
    boolean hasJpaEvidence,
    boolean hasExternalSystemEvidence,
    boolean hasFrontendEvidence,
    Map<EntityKind, Long> entityCountsByKind
) {
    static ViewpointEvidence from(
        List<ArchitectureEntity> entities,
        List<ArchitectureRelationship> relationships,
        Map<String, Object> dependencyViews
    ) {
        List<ArchitectureEntity> safeEntities = entities == null ? List.of() : List.copyOf(entities);
        List<ArchitectureRelationship> safeRelationships = relationships == null ? List.of() : List.copyOf(relationships);
        Map<String, Object> safeDependencyViews = dependencyViews == null ? Map.of() : Map.copyOf(dependencyViews);
        Map<String, List<String>> entityIdsByRole = safeEntities.stream()
            .filter(entity -> entity.architecturalRoles() != null)
            .flatMap(entity -> entity.architecturalRoles().stream().map(role -> Map.entry(role, entity.id())))
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.collectingAndThen(Collectors.toCollection(java.util.TreeSet::new), List::copyOf))
            ));
        Set<String> relationshipSemantics = safeRelationships.stream()
            .map(ArchitectureRelationship::architecturalSemantics)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        boolean hasJavaInterpretationEvidence = safeEntities.stream().anyMatch(entity -> hasJavaInterpretationMetadata(entity.metadata()))
            || safeRelationships.stream().anyMatch(relationship -> hasJavaInterpretationMetadata(relationship.metadata()));
        boolean hasJpaEvidence = safeEntities.stream().anyMatch(entity -> hasJpaMetadata(entity.metadata()));
        boolean hasExternalSystemEvidence = safeEntities.stream().anyMatch(entity -> entity.kind() == EntityKind.EXTERNAL_SYSTEM)
            || safeEntities.stream().anyMatch(entity -> hasExternalMetadata(entity.metadata()));
        boolean hasFrontendEvidence = safeEntities.stream().anyMatch(entity -> hasFrontendMetadata(entity.metadata()))
            || safeRelationships.stream().anyMatch(relationship -> hasFrontendMetadata(relationship.metadata()));
        Map<EntityKind, Long> entityCountsByKind = safeEntities.stream()
            .collect(Collectors.groupingBy(ArchitectureEntity::kind, Collectors.counting()));
        return new ViewpointEvidence(
            safeEntities,
            safeRelationships,
            safeDependencyViews,
            entityIdsByRole,
            relationshipSemantics,
            hasJavaInterpretationEvidence,
            hasJpaEvidence,
            hasExternalSystemEvidence,
            hasFrontendEvidence,
            entityCountsByKind
        );
    }

    List<String> entityIdsForRole(String roleId) {
        return entityIdsByRole.getOrDefault(roleId, List.of());
    }

    boolean hasSemantic(String semantic) {
        return relationshipSemantics.contains(semantic);
    }

    boolean hasEntityKind(EntityKind kind) {
        return entityCountsByKind.getOrDefault(kind, 0L) > 0;
    }

    long entityCountByKind(EntityKind kind) {
        return entityCountsByKind.getOrDefault(kind, 0L);
    }

    boolean hasDependencyViewList(String key) {
        Object value = dependencyViews.get(key);
        return value instanceof Collection<?> collection && !collection.isEmpty();
    }

    private static boolean hasJavaInterpretationMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        return metadata.containsKey("backendProfile")
            || containsIgnoreCase(metadata.get("frameworks"), "jax-rs")
            || containsIgnoreCase(metadata.get("frameworks"), "spring")
            || containsIgnoreCase(metadata.get("technology"), "java")
            || containsIgnoreCase(metadata.get("language"), "java");
    }

    private static boolean hasJpaMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        return containsIgnoreCase(metadata.get("frameworks"), "jpa")
            || containsIgnoreCase(metadata.get("annotations"), "@Entity")
            || containsIgnoreCase(metadata.get("backendProfile"), "jpa")
            || containsIgnoreCase(metadata.get("backendProfile"), "repository")
            || containsIgnoreCase(metadata.get("entityRole"), "repository");
    }

    private static boolean hasFrontendMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        return containsIgnoreCase(metadata.get("framework"), "react")
            || containsIgnoreCase(metadata.get("framework"), "angular")
            || containsIgnoreCase(metadata.get("frameworks"), "react")
            || containsIgnoreCase(metadata.get("frameworks"), "angular")
            || metadata.containsKey("routePath")
            || metadata.containsKey("routeDeclarationKind")
            || metadata.containsKey("routeSourceKind")
            || metadata.containsKey("navigationTargetLiteral")
            || metadata.containsKey("redirectTargetLiteral");
    }

    private static boolean hasExternalMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        return containsIgnoreCase(metadata.get("entityRole"), "external")
            || containsIgnoreCase(metadata.get("backendProfile"), "integration")
            || containsIgnoreCase(metadata.get("kindHint"), "external");
    }

    private static boolean containsIgnoreCase(Object value, String needle) {
        if (value == null) {
            return false;
        }
        String lowerNeedle = needle.toLowerCase(Locale.ROOT);
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(text -> text.toLowerCase(Locale.ROOT))
                .anyMatch(text -> text.contains(lowerNeedle));
        }
        String text = value.toString().toLowerCase(Locale.ROOT);
        return text.contains(lowerNeedle);
    }
}
