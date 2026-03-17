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
            Map<String, Object> identifiers = new LinkedHashMap<>();
            identifiers.put("sourcePackageName", sourcePackageName);
            identifiers.put("targetPackageName", targetPackageName);
            identifiers.put("relationshipKind", relationshipKind.name());
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metrics.put("sourceTypeCount", sourceTypeIds.size());
            metrics.put("targetTypeCount", targetTypeIds.size());
            Map<String, Object> flags = new LinkedHashMap<>();
            flags.put("sourceBoundary", sourceBoundary);
            flags.put("targetBoundary", targetBoundary);
            flags.put("targetPackageClassification", targetPackageClassification);
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
