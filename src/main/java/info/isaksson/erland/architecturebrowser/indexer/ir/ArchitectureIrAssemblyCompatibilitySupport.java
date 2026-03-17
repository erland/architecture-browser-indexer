package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class ArchitectureIrAssemblyCompatibilitySupport {
    private ArchitectureIrAssemblyCompatibilitySupport() {
    }

    static Map<String, ArchitectureEntity> observedTypesByQualifiedName(Map<String, ArchitectureEntity> entitiesById) {
        Map<String, ArchitectureEntity> observed = new LinkedHashMap<>();
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (!isTypeEntity(entity) || entity.origin() != EntityOrigin.OBSERVED) {
                continue;
            }
            String qualifiedName = qualifiedNameForEntity(entity);
            if (qualifiedName != null && !qualifiedName.isBlank()) {
                observed.putIfAbsent(qualifiedName, entity);
            }
        }
        return Map.copyOf(observed);
    }

    static ArchitectureEntity canonicalDependencyEntity(
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

    static boolean isTypeDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        return isDependencyRelationship(relationship.kind()) && isTypeEntity(source) && isTypeEntity(target);
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

    static boolean isPackageEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("package", entity.metadata().get("logicalRole"));
    }

    static String findPackageEntityIdByName(String packageName, Map<String, ArchitectureEntity> entitiesById) {
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

    static String normalizeScopeId(String scopeId, String repositoryScopeId) {
        return ArchitectureIrScopeNormalizationSupport.normalizeScopeId(scopeId, repositoryScopeId);
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

    private static boolean isDependencyRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.DEPENDS_ON
            || kind == RelationshipKind.EXTENDS
            || kind == RelationshipKind.IMPLEMENTS
            || kind == RelationshipKind.EXPOSES;
    }

    private static String packageNameFromQualifiedName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank() || !qualifiedName.contains(".")) {
            return null;
        }
        return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
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

    private static String stringMetadata(ArchitectureEntity entity, String key, String fallback) {
        if (entity == null || entity.metadata() == null) {
            return fallback;
        }
        Object value = entity.metadata().get(key);
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return fallback;
    }
}
