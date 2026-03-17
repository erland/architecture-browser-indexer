package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ArchitectureIrPackageDependencyViewBuilder {
    private ArchitectureIrPackageDependencyViewBuilder() {
    }

    static List<Map<String, Object>> build(List<ArchitectureIrNormalizedDependencyContext> contexts) {
        Map<String, NormalizedPackageDependency> packageDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureIrNormalizedDependencyContext context : contexts) {
            if (!context.typeDependencyRelationship()) {
                continue;
            }
            if (context.sourcePackageName() == null || context.targetPackageName() == null || Objects.equals(context.sourcePackageName(), context.targetPackageName())) {
                continue;
            }
            String packageKey = context.relationship().kind().name() + "|" + context.sourcePackageName() + "|" + context.targetPackageName();
            packageDependenciesByKey.computeIfAbsent(packageKey, ignored -> new NormalizedPackageDependency(
                context.sourcePackageName(),
                context.targetPackageName(),
                context.relationship().kind(),
                context.internalTarget(),
                context.externalTarget(),
                context.sourcePackageBoundary(),
                context.targetPackageBoundary(),
                context.targetPackageClassification()
            )).addEvidence(context.relationship(), context.source(), context.target());
        }
        List<Map<String, Object>> packageDependencies = new ArrayList<>();
        for (NormalizedPackageDependency dependency : packageDependenciesByKey.values()) {
            packageDependencies.add(dependency.toMetadataMap());
        }
        return List.copyOf(packageDependencies);
    }

    private static final class NormalizedPackageDependency {
        private final String sourcePackageName;
        private final String targetPackageName;
        private final RelationshipKind relationshipKind;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetPackageClassification;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();
        private final Set<String> sourceTypeIds = new LinkedHashSet<>();
        private final Set<String> targetTypeIds = new LinkedHashSet<>();

        private NormalizedPackageDependency(
            String sourcePackageName,
            String targetPackageName,
            RelationshipKind relationshipKind,
            boolean internalTarget,
            boolean externalTarget,
            String sourceBoundary,
            String targetBoundary,
            String targetPackageClassification
        ) {
            this.sourcePackageName = sourcePackageName;
            this.targetPackageName = targetPackageName;
            this.relationshipKind = relationshipKind;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetPackageClassification = targetPackageClassification;
        }

        private void addEvidence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
            if (source != null) {
                sourceTypeIds.add(source.id());
            }
            if (target != null) {
                targetTypeIds.add(target.id());
            }
            if (relationship.metadata() != null) {
                ArchitectureIrDependencyMetadataSupport.addIfPresent(dependencySources, relationship.metadata().get("dependencySource"));
                ArchitectureIrDependencyMetadataSupport.addIfPresent(dependencyCategories, relationship.metadata().get("dependencyCategory"));
                ArchitectureIrDependencyMetadataSupport.addFrameworkMetadata(frameworks, frameworkRelationships, architectureViewKinds, relationship);
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourcePackageName", sourcePackageName);
            metadata.put("targetPackageName", targetPackageName);
            metadata.put("relationshipKind", relationshipKind.name());
            ArchitectureIrDependencyMetadataSupport.putSummaryCollections(metadata, dependencySources, dependencyCategories, frameworks, frameworkRelationships, architectureViewKinds, evidenceRelationshipIds, evidenceLabels);
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetPackageClassification", targetPackageClassification);
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("sourceTypeCount", sourceTypeIds.size());
            metadata.put("targetTypeCount", targetTypeIds.size());
            return ArchitectureIrDependencyMetadataSupport.immutable(metadata);
        }
    }
}
