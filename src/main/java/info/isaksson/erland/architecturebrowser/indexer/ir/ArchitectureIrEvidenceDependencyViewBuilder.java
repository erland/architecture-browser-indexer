package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ArchitectureIrEvidenceDependencyViewBuilder {
    private ArchitectureIrEvidenceDependencyViewBuilder() {
    }

    static List<Map<String, Object>> build(List<ArchitectureIrNormalizedDependencyContext> contexts) {
        Map<String, EvidenceDependency> evidenceDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureIrNormalizedDependencyContext context : contexts) {
            if (!context.importEvidenceRelationship()) {
                continue;
            }
            String evidenceKey = context.relationship().kind().name() + "|" + context.evidenceSourceEntityId() + "|" + context.evidenceTargetEntityId();
            evidenceDependenciesByKey.computeIfAbsent(evidenceKey, ignored -> new EvidenceDependency(
                context.evidenceSourceEntityId(),
                context.evidenceTargetEntityId(),
                context.relationship().kind(),
                context.evidenceSourceName(),
                context.evidenceTargetName(),
                context.sourceBoundary(),
                context.targetBoundary(),
                context.evidenceTargetClassification()
            )).addEvidence(context.relationship());
        }
        List<Map<String, Object>> evidenceDependencies = new ArrayList<>();
        for (EvidenceDependency dependency : evidenceDependenciesByKey.values()) {
            evidenceDependencies.add(dependency.toMetadataMap());
        }
        return List.copyOf(evidenceDependencies);
    }

    private static final class EvidenceDependency {
        private final String sourceEntityId;
        private final String targetEntityId;
        private final RelationshipKind relationshipKind;
        private final String sourceName;
        private final String targetName;
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

        private EvidenceDependency(
            String sourceEntityId,
            String targetEntityId,
            RelationshipKind relationshipKind,
            String sourceName,
            String targetName,
            String sourceBoundary,
            String targetBoundary,
            String targetClassification
        ) {
            this.sourceEntityId = sourceEntityId;
            this.targetEntityId = targetEntityId;
            this.relationshipKind = relationshipKind;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetClassification = targetClassification;
        }

        private void addEvidence(ArchitectureRelationship relationship) {
            Object dependencySource = relationship.metadata() == null ? null : relationship.metadata().get("dependencySource");
            ArchitectureIrDependencyMetadataSupport.addIfPresent(dependencySources, dependencySource);
            Object dependencyCategory = relationship.metadata() == null ? null : relationship.metadata().get("dependencyCategory");
            ArchitectureIrDependencyMetadataSupport.addIfPresent(dependencyCategories, dependencyCategory);
            ArchitectureIrDependencyMetadataSupport.addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceEntityId", sourceEntityId);
            metadata.put("targetEntityId", targetEntityId);
            metadata.put("relationshipKind", relationshipKind.name());
            metadata.put("sourceName", sourceName);
            metadata.put("targetName", targetName);
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetClassification", targetClassification);
            ArchitectureIrDependencyMetadataSupport.putSummaryCollections(metadata, dependencySources, dependencyCategories, frameworks, frameworkRelationships, architectureViewKinds, evidenceRelationshipIds, evidenceLabels);
            metadata.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("dependencyTier", "supporting-evidence");
            metadata.put("architecturePrimary", false);
            metadata.put("recommendedForArchitectureViews", false);
            metadata.put("evidenceKind", "file-import");
            return ArchitectureIrDependencyMetadataSupport.immutable(metadata);
        }
    }
}
