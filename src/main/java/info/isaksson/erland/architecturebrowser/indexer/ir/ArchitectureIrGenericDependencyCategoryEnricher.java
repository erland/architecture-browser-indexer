package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.Map;
import java.util.Objects;

final class ArchitectureIrGenericDependencyCategoryEnricher {
    private ArchitectureIrGenericDependencyCategoryEnricher() {
    }

    static Map<String, Object> enrich(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        Map<String, Object> metadata,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        if (ArchitectureIrTypeDependencyCategoryEnricher.shouldEnrich(relationship, source, target)) {
            return ArchitectureIrTypeDependencyCategoryEnricher.enrich(relationship, source, target, metadata, entitiesById);
        }
        if (ArchitectureIrPackageDependencyCategoryEnricher.shouldEnrich(relationship, source, target)) {
            return ArchitectureIrPackageDependencyCategoryEnricher.enrich(relationship, source, target, metadata, entitiesById);
        }
        if (ArchitectureIrModuleDependencyCategoryEnricher.shouldEnrich(relationship, source, target)) {
            return ArchitectureIrModuleDependencyCategoryEnricher.enrich(relationship, source, target, metadata, entitiesById);
        }
        return metadata;
    }

    static boolean shouldEnrich(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return ArchitectureIrTypeDependencyCategoryEnricher.shouldEnrich(relationship, source, target)
            || ArchitectureIrPackageDependencyCategoryEnricher.shouldEnrich(relationship, source, target)
            || ArchitectureIrModuleDependencyCategoryEnricher.shouldEnrich(relationship, source, target);
    }

    static boolean isDependencyRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.DEPENDS_ON
            || kind == RelationshipKind.EXTENDS
            || kind == RelationshipKind.IMPLEMENTS
            || kind == RelationshipKind.EXPOSES;
    }

    static boolean hasRollup(ArchitectureRelationship relationship, String expectedRollup) {
        return relationship != null
            && relationship.metadata() != null
            && Objects.equals(expectedRollup, relationship.metadata().get("rollup"));
    }

    static boolean isTypeDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        return isDependencyRelationship(relationship.kind()) && isTypeEntity(source) && isTypeEntity(target);
    }

    static boolean isPackageDependencyRelationship(
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

    static boolean isModuleDependencyRelationship(
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

    static String packageNameForDependencyEntity(ArchitectureEntity entity) {
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

    static String moduleNameForDependencyEntity(ArchitectureEntity entity) {
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

    static String moduleBoundaryForName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
        if (moduleName == null || moduleName.isBlank()) {
            return "unknown";
        }
        return findSourceRootEntityIdByName(moduleName, entitiesById) != null ? "internal" : "external";
    }

    static String moduleClassificationForName(String moduleName, Map<String, ArchitectureEntity> entitiesById) {
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

    static String packageBoundaryForName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return "unknown";
        }
        return findPackageEntityIdByName(packageName, entitiesById) != null ? "internal" : "external";
    }

    static String packageClassificationForName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
        if (packageName == null || packageName.isBlank()) {
            return "unknown";
        }
        return findPackageEntityIdByName(packageName, entitiesById) != null ? "observed-source-package" : "external-package";
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

    static String boundaryForEntity(ArchitectureEntity entity) {
        if (isInternalEntity(entity)) {
            return "internal";
        }
        if (isExternalEntity(entity)) {
            return "external";
        }
        return "unknown";
    }

    static String typeClassificationForEntity(ArchitectureEntity entity) {
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

    static boolean isInternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return !Boolean.TRUE.equals(external) && entity.origin() == EntityOrigin.OBSERVED;
    }

    static boolean isExternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return Boolean.TRUE.equals(external) || entity.origin() == EntityOrigin.INFERRED;
    }

    private static String stringMetadata(ArchitectureEntity entity, String key, String defaultValue) {
        if (entity == null || entity.metadata() == null) {
            return defaultValue;
        }
        Object value = entity.metadata().get(key);
        return value instanceof String s && !s.isBlank() ? s : defaultValue;
    }

    static boolean isPackageEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("package", entity.metadata().get("logicalRole"));
    }

    static boolean isSourceRootEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("source-root", entity.metadata().get("logicalRole"));
    }

    static boolean isTypeEntity(ArchitectureEntity entity) {
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
