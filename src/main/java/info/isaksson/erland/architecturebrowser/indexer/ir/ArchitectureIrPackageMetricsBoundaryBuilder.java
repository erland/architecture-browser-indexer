package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ArchitectureIrPackageMetricsBoundaryBuilder {
    private ArchitectureIrPackageMetricsBoundaryBuilder() {
    }

    static List<Map<String, Object>> buildPackageMetrics(
        Map<String, ArchitectureEntity> entitiesById,
        List<Map<String, Object>> packageDependencies
    ) {
        Map<String, PackageMetrics> metricsByPackage = new LinkedHashMap<>();
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (isPackageEntity(entity)) {
                String packageName = entity.name();
                metricsByPackage.putIfAbsent(packageName, new PackageMetrics(
                    packageName,
                    stringMetadata(entity, "language", "unknown"),
                    deriveSourceRoot(packageName, entity.sourceRefs()),
                    boundaryForEntity(entity),
                    "observed-source-package"
                ));
            }
        }
        for (ArchitectureEntity entity : entitiesById.values()) {
            String packageName = packageNameForEntityMetrics(entity);
            PackageMetrics metrics = metricsByPackage.get(packageName);
            if (metrics == null) {
                continue;
            }
            metrics.observeEntity(entity);
        }
        for (Map<String, Object> dependency : packageDependencies) {
            Object sourcePackageName = dependency.get("sourcePackageName");
            Object targetPackageName = dependency.get("targetPackageName");
            if (sourcePackageName instanceof String s) {
                PackageMetrics sourceMetrics = metricsByPackage.get(s);
                if (sourceMetrics != null) {
                    sourceMetrics.observeOutgoingDependency();
                }
            }
            if (targetPackageName instanceof String s) {
                PackageMetrics targetMetrics = metricsByPackage.get(s);
                if (targetMetrics != null) {
                    targetMetrics.observeIncomingDependency();
                }
            }
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (PackageMetrics metrics : metricsByPackage.values()) {
            results.add(metrics.toMetadataMap());
        }
        return List.copyOf(results);
    }

    static Map<String, Object> buildBoundarySummary(
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> packageDependencies,
        List<Map<String, Object>> moduleDependencies
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("typeInternalCount", countBoundary(typeDependencies, "targetBoundary", "internal"));
        summary.put("typeExternalCount", countBoundary(typeDependencies, "targetBoundary", "external"));
        summary.put("packageInternalCount", countBoundary(packageDependencies, "targetBoundary", "internal"));
        summary.put("packageExternalCount", countBoundary(packageDependencies, "targetBoundary", "external"));
        summary.put("moduleInternalCount", countBoundary(moduleDependencies, "targetBoundary", "internal"));
        summary.put("moduleExternalCount", countBoundary(moduleDependencies, "targetBoundary", "external"));
        return Map.copyOf(summary);
    }

    private static int countBoundary(List<Map<String, Object>> dependencies, String key, String expectedValue) {
        int count = 0;
        for (Map<String, Object> dependency : dependencies) {
            if (Objects.equals(expectedValue, dependency.get(key))) {
                count++;
            }
        }
        return count;
    }

    private static String packageNameForEntityMetrics(ArchitectureEntity entity) {
        if (entity == null || isPackageEntity(entity) || !isInternalEntity(entity)) {
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
            Object ownerQualifiedName = entity.metadata().get("ownerQualifiedName");
            if (ownerQualifiedName instanceof String ownerQualified && !ownerQualified.isBlank()) {
                String derived = packageNameFromQualifiedName(ownerQualified);
                if (derived != null) {
                    return derived;
                }
            }
        }
        return packageNameFromQualifiedName(entity.name());
    }

    private static String stringMetadata(ArchitectureEntity entity, String key, String defaultValue) {
        if (entity == null || entity.metadata() == null) {
            return defaultValue;
        }
        Object value = entity.metadata().get(key);
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return defaultValue;
    }

    private static String deriveSourceRoot(String packageName, List<SourceReference> sourceRefs) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            return null;
        }
        String packagePath = packageName == null ? null : packageName.replace('.', '/');
        for (SourceReference ref : sourceRefs) {
            if (ref == null || ref.path() == null || ref.path().isBlank()) {
                continue;
            }
            String path = ref.path().replace('\\', '/');
            if (packagePath != null && !packagePath.isBlank()) {
                String marker = "/" + packagePath + "/";
                int idx = path.indexOf(marker);
                if (idx > 0) {
                    return path.substring(0, idx);
                }
                if (path.endsWith("/" + packagePath)) {
                    return path.substring(0, path.length() - packagePath.length() - 1);
                }
            }
            int slash = path.lastIndexOf('/');
            if (slash > 0) {
                return path.substring(0, slash);
            }
        }
        return null;
    }

    private static String packageNameFromQualifiedName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank() || !qualifiedName.contains(".")) {
            return null;
        }
        return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    }

    private static boolean isPackageEntity(ArchitectureEntity entity) {
        return entity != null
            && entity.kind() == EntityKind.MODULE
            && entity.metadata() != null
            && Objects.equals("package", entity.metadata().get("logicalRole"));
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

    private static String boundaryForEntity(ArchitectureEntity entity) {
        if (isInternalEntity(entity)) {
            return "internal";
        }
        if (isExternalEntity(entity)) {
            return "external";
        }
        return "unknown";
    }

    private static final class PackageMetrics {
        private final String packageName;
        private final String language;
        private final String sourceRoot;
        private final String packageBoundary;
        private final String packageClassification;
        private int declaredTypeCount;
        private int classCount;
        private int interfaceCount;
        private int enumCount;
        private int recordCount;
        private int fieldCount;
        private int functionCount;
        private int incomingDependencyCount;
        private int outgoingDependencyCount;

        private PackageMetrics(
            String packageName,
            String language,
            String sourceRoot,
            String packageBoundary,
            String packageClassification
        ) {
            this.packageName = packageName;
            this.language = language;
            this.sourceRoot = sourceRoot;
            this.packageBoundary = packageBoundary;
            this.packageClassification = packageClassification;
        }

        private void observeEntity(ArchitectureEntity entity) {
            if (entity == null) {
                return;
            }
            switch (entity.kind()) {
                case CLASS -> {
                    declaredTypeCount++;
                    Object declarationKind = entity.metadata() == null ? null : entity.metadata().get("declarationKind");
                    if (Objects.equals("enum", declarationKind)) {
                        enumCount++;
                    } else if (Objects.equals("record", declarationKind)) {
                        recordCount++;
                    } else {
                        classCount++;
                    }
                }
                case INTERFACE -> {
                    declaredTypeCount++;
                    interfaceCount++;
                }
                case FIELD -> fieldCount++;
                case FUNCTION -> functionCount++;
                default -> {
                }
            }
        }

        private void observeIncomingDependency() {
            incomingDependencyCount++;
        }

        private void observeOutgoingDependency() {
            outgoingDependencyCount++;
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("packageName", packageName);
            metadata.put("qualifiedName", packageName);
            metadata.put("language", language);
            if (sourceRoot != null && !sourceRoot.isBlank()) {
                metadata.put("sourceRoot", sourceRoot);
            }
            metadata.put("packageBoundary", packageBoundary);
            metadata.put("packageClassification", packageClassification);
            metadata.put("declaredTypeCount", declaredTypeCount);
            metadata.put("classCount", classCount);
            metadata.put("interfaceCount", interfaceCount);
            metadata.put("enumCount", enumCount);
            metadata.put("recordCount", recordCount);
            metadata.put("fieldCount", fieldCount);
            metadata.put("functionCount", functionCount);
            metadata.put("incomingDependencyCount", incomingDependencyCount);
            metadata.put("outgoingDependencyCount", outgoingDependencyCount);
            return Map.copyOf(metadata);
        }
    }
}
