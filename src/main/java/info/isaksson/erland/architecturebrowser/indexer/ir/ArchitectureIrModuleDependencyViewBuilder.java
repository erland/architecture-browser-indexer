package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ArchitectureIrModuleDependencyViewBuilder {
    private ArchitectureIrModuleDependencyViewBuilder() {
    }

    static List<Map<String, Object>> build(List<ArchitectureIrNormalizedDependencyContext> contexts) {
        Map<String, NormalizedModuleDependency> moduleDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureIrNormalizedDependencyContext context : contexts) {
            if (!context.typeDependencyRelationship()) {
                continue;
            }
            if (context.sourceModuleName() == null || context.targetModuleName() == null) {
                continue;
            }
            String moduleKey = context.relationship().kind().name() + "|" + context.sourceModuleName() + "|" + context.targetModuleName();
            moduleDependenciesByKey.computeIfAbsent(moduleKey, ignored -> new NormalizedModuleDependency(
                context.sourceModuleName(),
                context.targetModuleName(),
                context.relationship().kind(),
                context.internalTarget(),
                context.externalTarget(),
                context.sourceModuleBoundary(),
                context.targetModuleBoundary(),
                context.targetModuleClassification(),
                context.sameModule()
            )).addEvidence(context.relationship(), context.source(), context.target());
        }
        List<Map<String, Object>> moduleDependencies = new ArrayList<>();
        for (NormalizedModuleDependency dependency : moduleDependenciesByKey.values()) {
            moduleDependencies.add(dependency.toMetadataMap());
        }
        return List.copyOf(moduleDependencies);
    }

    private static final class NormalizedModuleDependency {
        private final String sourceModuleName;
        private final String targetModuleName;
        private final RelationshipKind relationshipKind;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final String sourceBoundary;
        private final String targetBoundary;
        private final String targetModuleClassification;
        private final boolean sameModule;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> frameworks = new LinkedHashSet<>();
        private final Set<String> frameworkRelationships = new LinkedHashSet<>();
        private final Set<String> architectureViewKinds = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();
        private final Set<String> sourceTypeIds = new LinkedHashSet<>();
        private final Set<String> targetTypeIds = new LinkedHashSet<>();

        private NormalizedModuleDependency(
            String sourceModuleName,
            String targetModuleName,
            RelationshipKind relationshipKind,
            boolean internalTarget,
            boolean externalTarget,
            String sourceBoundary,
            String targetBoundary,
            String targetModuleClassification,
            boolean sameModule
        ) {
            this.sourceModuleName = sourceModuleName;
            this.targetModuleName = targetModuleName;
            this.relationshipKind = relationshipKind;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
            this.sourceBoundary = sourceBoundary;
            this.targetBoundary = targetBoundary;
            this.targetModuleClassification = targetModuleClassification;
            this.sameModule = sameModule;
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
            metadata.put("sourceModuleName", sourceModuleName);
            metadata.put("targetModuleName", targetModuleName);
            metadata.put("relationshipKind", relationshipKind.name());
            ArchitectureIrDependencyMetadataSupport.putSummaryCollections(metadata, dependencySources, dependencyCategories, frameworks, frameworkRelationships, architectureViewKinds, evidenceRelationshipIds, evidenceLabels);
            metadata.put("sourceBoundary", sourceBoundary);
            metadata.put("targetBoundary", targetBoundary);
            metadata.put("targetModuleClassification", targetModuleClassification);
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("sameModule", sameModule);
            metadata.put("underlyingRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("sourceTypeCount", sourceTypeIds.size());
            metadata.put("targetTypeCount", targetTypeIds.size());
            return ArchitectureIrDependencyMetadataSupport.immutable(metadata);
        }
    }
}
