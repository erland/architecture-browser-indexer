package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ArchitectureIrDependencyViewAssemblySupport {
    private ArchitectureIrDependencyViewAssemblySupport() {
    }

    static Map<String, Object> buildDependencyViews(ArchitectureIrDependencyViewAssemblyInputs inputs) {
        List<ArchitectureRelationship> relationships = inputs.relationships();
        Map<String, ArchitectureEntity> entitiesById = inputs.entitiesById();
        Map<String, ArchitectureEntity> observedTypesByQualifiedName = inputs.observedTypesByQualifiedName();
        Map<String, NormalizedTypeDependency> typeDependenciesByKey = new LinkedHashMap<>();
        Map<String, NormalizedPackageDependency> packageDependenciesByKey = new LinkedHashMap<>();
        Map<String, NormalizedModuleDependency> moduleDependenciesByKey = new LinkedHashMap<>();
        Map<String, EvidenceDependency> evidenceDependenciesByKey = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity rawSource = entitiesById.get(relationship.fromEntityId());
            ArchitectureEntity rawTarget = entitiesById.get(relationship.toEntityId());
            ArchitectureEntity source = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(rawSource, observedTypesByQualifiedName);
            ArchitectureEntity target = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(rawTarget, observedTypesByQualifiedName);
            if (isImportEvidenceRelationship(relationship, rawSource, rawTarget)) {
                String sourceEntityId = rawSource == null ? relationship.fromEntityId() : rawSource.id();
                String targetEntityId = rawTarget == null ? relationship.toEntityId() : rawTarget.id();
                String evidenceKey = relationship.kind().name() + "|" + sourceEntityId + "|" + targetEntityId;
                evidenceDependenciesByKey.computeIfAbsent(evidenceKey, ignored -> new EvidenceDependency(
                    sourceEntityId,
                    targetEntityId,
                    relationship.kind(),
                    rawSource == null ? null : rawSource.name(),
                    rawTarget == null ? null : rawTarget.name(),
                    boundaryForEntity(rawSource),
                    boundaryForEntity(rawTarget),
                    typeClassificationForEntity(rawTarget)
                )).addEvidence(relationship);
            }
            if (ArchitectureIrAssemblyCompatibilitySupport.isTypeDependencyRelationship(relationship, source, target)) {
                String sourceTypeId = source == null ? relationship.fromEntityId() : source.id();
                String targetTypeId = target == null ? relationship.toEntityId() : target.id();
                String typeKey = relationship.kind().name() + "|" + sourceTypeId + "|" + targetTypeId;
                typeDependenciesByKey.computeIfAbsent(typeKey, ignored -> new NormalizedTypeDependency(
                    sourceTypeId,
                    targetTypeId,
                    relationship.kind(),
                    qualifiedNameForEntity(source),
                    qualifiedNameForEntity(target),
                    isInternalEntity(target),
                    isExternalEntity(target),
                    boundaryForEntity(source),
                    boundaryForEntity(target),
                    typeClassificationForEntity(target)
                )).addEvidence(relationship);

                String sourcePackageName = ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(source);
                String targetPackageName = ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(target);
                if (sourcePackageName != null && targetPackageName != null && !sourcePackageName.equals(targetPackageName)) {
                    String packageKey = relationship.kind().name() + "|" + sourcePackageName + "|" + targetPackageName;
                    packageDependenciesByKey.computeIfAbsent(packageKey, ignored -> new NormalizedPackageDependency(
                        sourcePackageName,
                        targetPackageName,
                        relationship.kind(),
                        isInternalEntity(target),
                        isExternalEntity(target),
                        ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(sourcePackageName, entitiesById),
                        ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(targetPackageName, entitiesById),
                        ArchitectureIrAssemblyCompatibilitySupport.packageClassificationForName(targetPackageName, entitiesById)
                    )).addEvidence(relationship, source, target);
                }
                String sourceModuleName = moduleNameForDependencyEntity(source);
                String targetModuleName = moduleNameForDependencyEntity(target);
                if (sourceModuleName != null && targetModuleName != null) {
                    String moduleKey = relationship.kind().name() + "|" + sourceModuleName + "|" + targetModuleName;
                    moduleDependenciesByKey.computeIfAbsent(moduleKey, ignored -> new NormalizedModuleDependency(
                        sourceModuleName,
                        targetModuleName,
                        relationship.kind(),
                        isInternalEntity(target),
                        isExternalEntity(target),
                        moduleBoundaryForName(sourceModuleName, entitiesById),
                        moduleBoundaryForName(targetModuleName, entitiesById),
                        moduleClassificationForName(targetModuleName, entitiesById),
                        Objects.equals(sourceModuleName, targetModuleName)
                    )).addEvidence(relationship, source, target);
                }
            }
        }
        List<Map<String, Object>> typeDependencies = new ArrayList<>();
        for (NormalizedTypeDependency dependency : typeDependenciesByKey.values()) {
            typeDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> packageDependencies = new ArrayList<>();
        for (NormalizedPackageDependency dependency : packageDependenciesByKey.values()) {
            packageDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> moduleDependencies = new ArrayList<>();
        for (NormalizedModuleDependency dependency : moduleDependenciesByKey.values()) {
            moduleDependencies.add(dependency.toMetadataMap());
        }
        List<Map<String, Object>> evidenceDependencies = new ArrayList<>();
        for (EvidenceDependency dependency : evidenceDependenciesByKey.values()) {
            evidenceDependencies.add(dependency.toMetadataMap());
        }
        return ArchitectureIrDependencyViewPostProcessor.finalizeDependencyViews(
            entitiesById,
            List.copyOf(typeDependencies),
            List.copyOf(packageDependencies),
            List.copyOf(moduleDependencies),
            List.copyOf(evidenceDependencies)
        );
    }

    private static String qualifiedNameForEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.metadata() != null) {
            Object qualifiedName = entity.metadata().get("qualifiedName");
            if (qualifiedName instanceof String q && !q.isBlank()) {
                return q;
            }
        }
        String name = entity.name();
        return (name == null || name.isBlank()) ? null : name;
    }

    private static boolean isImportEvidenceRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (relationship.kind() != RelationshipKind.DEPENDS_ON || source == null || target == null) {
            return false;
        }
        Object dependencySource = relationship.metadata() == null ? null : relationship.metadata().get("dependencySource");
        return source.kind() == EntityKind.MODULE && target.kind() == EntityKind.MODULE && Objects.equals("import", dependencySource);
    }

    private static String moduleNameForDependencyEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return null;
        }
        if (isSourceRootEntity(entity)) {
            return entity.name();
        }
        if (entity.metadata() != null) {
            Object explicit = entity.metadata().get("sourceRoot");
            if (explicit instanceof String s && !s.isBlank()) {
                return s;
            }
            Object relativePath = entity.metadata().get("relativePath");
            if (relativePath instanceof String s && !s.isBlank()) {
                String derived = moduleRootFromRelativePath(s);
                if (derived != null) {
                    return derived;
                }
            }
        }
        if (isInternalEntity(entity)) {
            for (SourceReference ref : entity.sourceRefs()) {
                if (ref == null || ref.path() == null || ref.path().isBlank()) {
                    continue;
                }
                String derived = moduleRootFromRelativePath(ref.path());
                if (derived != null) {
                    return derived;
                }
            }
        }
        return ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(entity);
    }

    private static String moduleBoundaryForName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        if (moduleName == null || moduleName.isBlank()) {
            return "unknown";
        }
        return findSourceRootEntityIdByName(moduleName, entitiesById) != null ? "internal" : "external";
    }

    private static String moduleClassificationForName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        if (moduleName == null || moduleName.isBlank()) {
            return "unknown";
        }
        return findSourceRootEntityIdByName(moduleName, entitiesById) != null ? "observed-source-root" : "external-module-or-package";
    }

    private static String findSourceRootEntityIdByName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (isSourceRootEntity(entity) && Objects.equals(moduleName, entity.name())) {
                return entity.id();
            }
        }
        return null;
    }

    private static String moduleRootFromRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length >= 3 && "src".equals(parts[0]) && ("main".equals(parts[1]) || "test".equals(parts[1]))) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        return parts.length > 0 ? parts[0] : null;
    }

    private static boolean isSourceRootEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("source-root", entity.metadata().get("logicalRole"));
    }

    private static String boundaryForEntity(ArchitectureEntity entity) {
        if (isInternalEntity(entity)) {
            return "internal";
        }
        if (isExternalEntity(entity)) {
            return "external";
        }
        return "unknown";
    }

    private static String typeClassificationForEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return "unknown";
        }
        if (isInternalEntity(entity)) {
            return "observed-source-type";
        }
        if (isExternalEntity(entity)) {
            return "external-or-inferred-type";
        }
        return "unknown";
    }

    private static boolean isInternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return !Boolean.TRUE.equals(external) && entity.origin() == EntityOrigin.OBSERVED;
    }

    private static boolean isExternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return Boolean.TRUE.equals(external) || entity.origin() == EntityOrigin.INFERRED;
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


static String normalizeScopeId(String scopeId, String repositoryScopeId) {
    return ArchitectureIrScopeNormalizationSupport.normalizeScopeId(scopeId, repositoryScopeId);
}

private static String stringMetadata(ArchitectureEntity entity, String key, String defaultValue) {
    if (entity == null || entity.metadata() == null) {
        return defaultValue;
    }
    Object value = entity.metadata().get(key);
    return value instanceof String s && !s.isBlank() ? s : defaultValue;
}
}
