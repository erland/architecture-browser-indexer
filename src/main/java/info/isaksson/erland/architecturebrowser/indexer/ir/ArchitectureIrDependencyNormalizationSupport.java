package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ArchitectureIrDependencyNormalizationSupport {
    private ArchitectureIrDependencyNormalizationSupport() {
    }

    static List<ArchitectureIrNormalizedDependencyContext> normalize(ArchitectureIrDependencyViewAssemblyInputs inputs) {
        List<ArchitectureIrNormalizedDependencyContext> contexts = new ArrayList<>();
        Map<String, ArchitectureEntity> entitiesById = inputs.entitiesById();
        Map<String, ArchitectureEntity> observedTypesByQualifiedName = inputs.observedTypesByQualifiedName();
        for (ArchitectureRelationship relationship : inputs.relationships()) {
            ArchitectureEntity rawSource = entitiesById.get(relationship.fromEntityId());
            ArchitectureEntity rawTarget = entitiesById.get(relationship.toEntityId());
            ArchitectureEntity source = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(rawSource, observedTypesByQualifiedName);
            ArchitectureEntity target = ArchitectureIrAssemblyCompatibilitySupport.canonicalDependencyEntity(rawTarget, observedTypesByQualifiedName);
            boolean importEvidenceRelationship = isImportEvidenceRelationship(relationship, rawSource, rawTarget);
            boolean typeDependencyRelationship = ArchitectureIrAssemblyCompatibilitySupport.isTypeDependencyRelationship(relationship, source, target);

            String sourceTypeId = source == null ? relationship.fromEntityId() : source.id();
            String targetTypeId = target == null ? relationship.toEntityId() : target.id();
            String sourceTypeName = qualifiedNameForEntity(source);
            String targetTypeName = qualifiedNameForEntity(target);
            boolean internalTarget = isInternalEntity(target);
            boolean externalTarget = isExternalEntity(target);
            String sourceBoundary = boundaryForEntity(source);
            String targetBoundary = boundaryForEntity(target);
            String targetTypeClassification = typeClassificationForEntity(target);

            String sourcePackageName = ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(source);
            String targetPackageName = ArchitectureIrAssemblyCompatibilitySupport.packageNameForDependencyEntity(target);
            String sourcePackageBoundary = ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(sourcePackageName, entitiesById);
            String targetPackageBoundary = ArchitectureIrAssemblyCompatibilitySupport.packageBoundaryForName(targetPackageName, entitiesById);
            String targetPackageClassification = ArchitectureIrAssemblyCompatibilitySupport.packageClassificationForName(targetPackageName, entitiesById);

            String sourceModuleName = moduleNameForDependencyEntity(source);
            String targetModuleName = moduleNameForDependencyEntity(target);
            String sourceModuleBoundary = moduleBoundaryForName(sourceModuleName, entitiesById);
            String targetModuleBoundary = moduleBoundaryForName(targetModuleName, entitiesById);
            String targetModuleClassification = moduleClassificationForName(targetModuleName, entitiesById);
            boolean sameModule = Objects.equals(sourceModuleName, targetModuleName);

            String evidenceSourceEntityId = rawSource == null ? relationship.fromEntityId() : rawSource.id();
            String evidenceTargetEntityId = rawTarget == null ? relationship.toEntityId() : rawTarget.id();
            String evidenceSourceName = rawSource == null ? null : rawSource.name();
            String evidenceTargetName = rawTarget == null ? null : rawTarget.name();
            String evidenceTargetClassification = typeClassificationForEntity(rawTarget);

            contexts.add(new ArchitectureIrNormalizedDependencyContext(
                relationship,
                rawSource,
                rawTarget,
                source,
                target,
                importEvidenceRelationship,
                typeDependencyRelationship,
                sourceTypeId,
                targetTypeId,
                sourceTypeName,
                targetTypeName,
                internalTarget,
                externalTarget,
                sourceBoundary,
                targetBoundary,
                targetTypeClassification,
                sourcePackageName,
                targetPackageName,
                sourcePackageBoundary,
                targetPackageBoundary,
                targetPackageClassification,
                sourceModuleName,
                targetModuleName,
                sourceModuleBoundary,
                targetModuleBoundary,
                targetModuleClassification,
                sameModule,
                evidenceSourceEntityId,
                evidenceTargetEntityId,
                evidenceSourceName,
                evidenceTargetName,
                evidenceTargetClassification
            ));
        }
        return List.copyOf(contexts);
    }

    static String qualifiedNameForEntity(ArchitectureEntity entity) {
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
}
