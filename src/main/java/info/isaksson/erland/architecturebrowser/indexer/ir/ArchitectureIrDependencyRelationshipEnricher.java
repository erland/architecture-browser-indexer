package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ArchitectureIrDependencyRelationshipEnricher {
    private ArchitectureIrDependencyRelationshipEnricher() {
    }
    static List<ArchitectureRelationship> enrichDependencyRelationshipMetadata(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        List<ArchitectureRelationship> enriched = new ArrayList<>(relationships.size());
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = canonicalDependencyEntity(entitiesById.get(relationship.fromEntityId()), observedTypesByQualifiedName);
            ArchitectureEntity target = canonicalDependencyEntity(entitiesById.get(relationship.toEntityId()), observedTypesByQualifiedName);
            boolean packageRollup = hasRollup(relationship, "package-package");
            boolean moduleRollup = hasRollup(relationship, "module-module");
            boolean dependencyRelationship = isDependencyRelationship(relationship.kind());
            boolean packageDependencyRelationship = packageRollup || isPackageDependencyRelationship(relationship, source, target);
            boolean moduleDependencyRelationship = moduleRollup || isModuleDependencyRelationship(relationship, source, target);
            if (!dependencyRelationship && !packageDependencyRelationship && !moduleDependencyRelationship) {
                enriched.add(relationship);
                continue;
            }
            Map<String, Object> metadata = ArchitectureIrDependencyMetadataSupport.mutableCopy(relationship.metadata());
            if (isTypeDependencyRelationship(relationship, source, target)) {
                metadata.put("dependencyView", "type");
                metadata.put("dependencySourceTypeId", source == null ? relationship.fromEntityId() : source.id());
                metadata.put("dependencyTargetTypeId", target == null ? relationship.toEntityId() : target.id());
                metadata.put("dependencySourceBoundary", boundaryForEntity(source));
                metadata.put("dependencyTargetBoundary", boundaryForEntity(target));
                metadata.put("dependencyTargetInternal", isInternalEntity(target));
                metadata.put("dependencyTargetExternal", isExternalEntity(target));
                metadata.put("dependencyTargetClassification", typeClassificationForEntity(target));
                String sourcePackageName = packageNameForDependencyEntity(source);
                String targetPackageName = packageNameForDependencyEntity(target);
                if (sourcePackageName != null) {
                    metadata.put("dependencySourcePackageName", sourcePackageName);
                    metadata.put("dependencySourcePackageBoundary", packageBoundaryForName(sourcePackageName, entitiesById));
                }
                if (targetPackageName != null) {
                    metadata.put("dependencyTargetPackageName", targetPackageName);
                    metadata.put("dependencyTargetPackageBoundary", packageBoundaryForName(targetPackageName, entitiesById));
                    metadata.put("dependencyTargetPackageClassification", packageClassificationForName(targetPackageName, entitiesById));
                }
            } else if (packageDependencyRelationship) {
                metadata.put("dependencyView", "package");
                metadata.put("dependencySourcePackageId", relationship.fromEntityId());
                metadata.put("dependencyTargetPackageId", relationship.toEntityId());
                String sourcePackageName = source == null ? null : source.name();
                String targetPackageName = target == null ? null : target.name();
                if (sourcePackageName != null) {
                    metadata.put("dependencySourcePackageName", sourcePackageName);
                    metadata.put("dependencySourcePackageBoundary", packageBoundaryForName(sourcePackageName, entitiesById));
                }
                if (targetPackageName != null) {
                    metadata.put("dependencyTargetPackageName", targetPackageName);
                    metadata.put("dependencyTargetPackageBoundary", packageBoundaryForName(targetPackageName, entitiesById));
                    metadata.put("dependencyTargetPackageClassification", packageClassificationForName(targetPackageName, entitiesById));
                }
                metadata.put("dependencyTargetBoundary", targetPackageName == null ? "unknown" : packageBoundaryForName(targetPackageName, entitiesById));
            } else if (moduleDependencyRelationship) {
                metadata.put("dependencyView", "module");
                metadata.put("dependencySourceModuleId", relationship.fromEntityId());
                metadata.put("dependencyTargetModuleId", relationship.toEntityId());
                String sourceModuleName = source == null ? null : moduleNameForDependencyEntity(source);
                String targetModuleName = target == null ? null : moduleNameForDependencyEntity(target);
                if (sourceModuleName != null) {
                    metadata.put("dependencySourceModuleName", sourceModuleName);
                    metadata.put("dependencySourceModuleBoundary", moduleBoundaryForName(sourceModuleName, entitiesById));
                }
                if (targetModuleName != null) {
                    metadata.put("dependencyTargetModuleName", targetModuleName);
                    metadata.put("dependencyTargetModuleBoundary", moduleBoundaryForName(targetModuleName, entitiesById));
                    metadata.put("dependencyTargetModuleClassification", moduleClassificationForName(targetModuleName, entitiesById));
                }
                metadata.put("dependencyTargetBoundary", targetModuleName == null ? "unknown" : moduleBoundaryForName(targetModuleName, entitiesById));
                metadata.put("sameModule", Objects.equals(sourceModuleName, targetModuleName));
            } else if (isImportEvidenceRelationship(relationship, source, target)) {
                metadata = ArchitectureIrDependencyMetadataSupport.shapeImportEvidenceMetadata(
                    relationship,
                    source,
                    target,
                    metadata,
                    isInternalEntity(target),
                    isExternalEntity(target),
                    boundaryForEntity(target),
                    typeClassificationForEntity(target)
                );
            }
            enriched.add(new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                ArchitectureIrDependencyMetadataSupport.immutable(metadata)
            ));
        }
        return List.copyOf(enriched);
    }


    private static ArchitectureEntity canonicalDependencyEntity(
        ArchitectureEntity entity,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        if (entity == null || !isTypeEntity(entity) || entity.origin() == EntityOrigin.OBSERVED) {
            return entity;
        }
        String qualifiedName = qualifiedNameForEntity(entity);
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return entity;
        }
        return observedTypesByQualifiedName.getOrDefault(qualifiedName, entity);
    }

    private static boolean hasRollup(ArchitectureRelationship relationship, String expectedRollup) {
        return relationship != null
            && relationship.metadata() != null
            && Objects.equals(expectedRollup, relationship.metadata().get("rollup"));
    }

    private static boolean isDependencyRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.DEPENDS_ON
            || kind == RelationshipKind.EXTENDS
            || kind == RelationshipKind.IMPLEMENTS
            || kind == RelationshipKind.EXPOSES;
    }

    private static boolean isTypeDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        return isDependencyRelationship(relationship.kind()) && isTypeEntity(source) && isTypeEntity(target);
    }

    private static boolean isPackageDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (source == null || target == null) {
            return false;
        }
        Object rollup = relationship.metadata() == null ? null : relationship.metadata().get("rollup");
        return (relationship.kind() == RelationshipKind.USES || isDependencyRelationship(relationship.kind()))
            && Objects.equals("package-package", rollup)
            && isPackageEntity(source)
            && isPackageEntity(target);
    }

    private static boolean isModuleDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (source == null || target == null) {
            return false;
        }
        Object rollup = relationship.metadata() == null ? null : relationship.metadata().get("rollup");
        return (relationship.kind() == RelationshipKind.USES || isDependencyRelationship(relationship.kind()))
            && Objects.equals("module-module", rollup)
            && isSourceRootEntity(source)
            && isSourceRootEntity(target);
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

    private static String packageNameForDependencyEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.metadata() != null) {
            Object explicitPackage = entity.metadata().get("packageName");
            if (explicitPackage instanceof String packageName && !packageName.isBlank()) {
                return packageName;
            }
            Object qualifiedName = entity.metadata().get("qualifiedName");
            if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
                String derived = packageNameFromQualifiedName(qualified);
                if (derived != null) {
                    return derived;
                }
            }
        }
        return packageNameFromQualifiedName(entity.name());
    }

    private static String packageNameFromQualifiedName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        return qualifiedName.substring(0, lastDot);
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
        return packageNameForDependencyEntity(entity);
    }


    private static String findPackageEntityIdByName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (isPackageEntity(entity) && packageName.equals(entity.name())) {
                return entity.id();
            }
        }
        return null;
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
        int slash = relativePath.indexOf('/');
        if (slash > 0) {
            return relativePath.substring(0, slash);
        }
        return relativePath;
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

    private static String packageBoundaryForName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return "unknown";
        }
        return findPackageEntityIdByName(packageName, entitiesById) != null ? "internal" : "external";
    }

    private static String packageClassificationForName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return "unknown";
        }
        return findPackageEntityIdByName(packageName, entitiesById) != null ? "observed-source-package" : "external-package";
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


    private static String stringMetadata(ArchitectureEntity entity, String key, String defaultValue) {
        if (entity == null || entity.metadata() == null) {
            return defaultValue;
        }
        Object value = entity.metadata().get(key);
        return value instanceof String s && !s.isBlank() ? s : defaultValue;
    }

    private static boolean isPackageEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("package", entity.metadata().get("logicalRole"));
    }

    private static boolean isTypeEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE || entity.kind() == EntityKind.ENDPOINT) {
            return true;
        }
        if (entity.kind() == EntityKind.FUNCTION || entity.kind() == EntityKind.UI_MODULE) {
            String language = stringMetadata(entity, "language", "");
            if ("typescript".equalsIgnoreCase(language)) {
                return true;
            }
            if (entity.metadata() != null && entity.metadata().get("framework") != null) {
                return true;
            }
        }
        if (entity.kind() == EntityKind.MODULE && entity.metadata() != null) {
            String targetClassification = stringMetadata(entity, "targetClassification", "");
            if ("angular-di-token".equals(targetClassification)) {
                return true;
            }
            if (Boolean.TRUE.equals(entity.metadata().get("angularToken"))) {
                return true;
            }
            if (Boolean.TRUE.equals(entity.metadata().get("angularDiValue"))) {
                return true;
            }
        }
        return false;
    }

}
