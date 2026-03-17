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
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceTypeId", sourceTypeId);
            metadata.put("targetTypeId", targetTypeId);
            metadata.put("relationshipKind", relationshipKind.name());
            if (sourceTypeName != null) {
                metadata.put("sourceTypeName", sourceTypeName);
            }
            if (targetTypeName != null) {
                metadata.put("targetTypeName", targetTypeName);
            }
            ArchitectureIrDependencyMetadataSupport.putSummaryCollections(metadata, dependencySources, dependencyCategories, frameworks, frameworkRelationships, architectureViewKinds, evidenceRelationshipIds, evidenceLabels);
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetClassification", targetClassification);
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("evidenceRelationshipCount", evidenceRelationshipIds.size());
            return ArchitectureIrDependencyMetadataSupport.immutable(metadata);
        }
    }
}
