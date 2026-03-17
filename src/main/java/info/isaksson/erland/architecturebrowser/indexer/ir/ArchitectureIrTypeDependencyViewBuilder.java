package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ArchitectureIrTypeDependencyViewBuilder {
    private ArchitectureIrTypeDependencyViewBuilder() {
    }

    static List<Map<String, Object>> build(List<ArchitectureIrNormalizedDependencyContext> contexts) {
        Map<String, NormalizedTypeDependency> typeDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureIrNormalizedDependencyContext context : contexts) {
            if (!context.typeDependencyRelationship()) {
                continue;
            }
            String typeKey = context.relationship().kind().name() + "|" + context.sourceTypeId() + "|" + context.targetTypeId();
            typeDependenciesByKey.computeIfAbsent(typeKey, ignored -> new NormalizedTypeDependency(
                context.sourceTypeId(),
                context.targetTypeId(),
                context.relationship().kind(),
                context.sourceTypeName(),
                context.targetTypeName(),
                context.internalTarget(),
                context.externalTarget(),
                context.sourceBoundary(),
                context.targetBoundary(),
                context.targetTypeClassification()
            )).addEvidence(context.relationship());
        }
        List<Map<String, Object>> typeDependencies = new ArrayList<>();
        for (NormalizedTypeDependency dependency : typeDependenciesByKey.values()) {
            typeDependencies.add(dependency.toMetadataMap());
        }
        return List.copyOf(typeDependencies);
    }

    private static final class NormalizedTypeDependency {
        private final String sourceTypeId;
        private final String targetTypeId;
        private final RelationshipKind relationshipKind;
        private final String sourceTypeName;
        private final String targetTypeName;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetClassification;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();

        private NormalizedTypeDependency(
            String sourceTypeId,
            String targetTypeId,
            RelationshipKind relationshipKind,
            String sourceTypeName,
            String targetTypeName,
            boolean internalTarget,
            boolean externalTarget,
            String sourceBoundary,
            String targetBoundary,
            String targetClassification
        ) {
            this.sourceTypeId = sourceTypeId;
            this.targetTypeId = targetTypeId;
            this.relationshipKind = relationshipKind;
            this.sourceTypeName = sourceTypeName;
            this.targetTypeName = targetTypeName;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetClassification = targetClassification;
        }

        private void addEvidence(ArchitectureRelationship relationship) {
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
            if (relationship.metadata() != null) {
                ArchitectureIrDependencyMetadataSupport.addIfPresent(dependencySources, relationship.metadata().get("dependencySource"));
                ArchitectureIrDependencyMetadataSupport.addIfPresent(dependencyCategories, relationship.metadata().get("dependencyCategory"));
                ArchitectureIrDependencyMetadataSupport.addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> identifiers = new LinkedHashMap<>();
            identifiers.put("sourceTypeId", sourceTypeId);
            identifiers.put("targetTypeId", targetTypeId);
            identifiers.put("relationshipKind", relationshipKind.name());
            if (sourceTypeName != null) {
                identifiers.put("sourceTypeName", sourceTypeName);
            }
            if (targetTypeName != null) {
                identifiers.put("targetTypeName", targetTypeName);
            }
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("evidenceRelationshipCount", evidenceRelationshipIds.size());
            Map<String, Object> flags = new LinkedHashMap<>();
            flags.put("sourceBoundary", sourceBoundary);
            flags.put("targetBoundary", targetBoundary);
            flags.put("targetClassification", targetClassification);
            flags.put("internalTarget", internalTarget);
            flags.put("externalTarget", externalTarget);
            return DependencyViewEntry.of(
                identifiers,
                new DependencyViewEntry.DependencyViewSummary(
                    List.copyOf(dependencySources),
                    List.copyOf(dependencyCategories),
                    List.copyOf(frameworks),
                    List.copyOf(frameworkRelationships),
                    List.copyOf(architectureViewKinds),
                    List.copyOf(evidenceRelationshipIds),
                    List.copyOf(evidenceLabels)
                ),
                metrics,
                flags
            ).toMetadataMap();
        }
    }
}
