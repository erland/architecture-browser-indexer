package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ArchitectureIrPackageEntityEnrichmentSupport {
    private ArchitectureIrPackageEntityEnrichmentSupport() {
    }

    static Map<String, ArchitectureEntity> enrichPackageEntities(
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, Object> dependencyViews
    ) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> packageMetrics = dependencyViews == null
            ? List.of()
            : (List<Map<String, Object>>) dependencyViews.getOrDefault("packageMetrics", List.of());
        if (packageMetrics.isEmpty()) {
            return entitiesById;
        }
        Map<String, Map<String, Object>> metricsByPackageName = new LinkedHashMap<>();
        for (Map<String, Object> metric : packageMetrics) {
            Object packageName = metric.get("packageName");
            if (packageName instanceof String s && !s.isBlank()) {
                metricsByPackageName.put(s, metric);
            }
        }
        Map<String, ArchitectureEntity> enriched = new LinkedHashMap<>();
        for (ArchitectureEntity entity : entitiesById.values()) {
            if (!ArchitectureIrAssemblyCompatibilitySupport.isPackageEntity(entity)) {
                enriched.put(entity.id(), entity);
                continue;
            }
            Map<String, Object> metric = metricsByPackageName.get(entity.name());
            if (metric == null) {
                enriched.put(entity.id(), entity);
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (entity.metadata() != null) {
                metadata.putAll(entity.metadata());
            }
            metadata.putAll(metric);
            enriched.put(entity.id(), new ArchitectureEntity(
                entity.id(),
                entity.kind(),
                entity.origin(),
                entity.name(),
                entity.displayName(),
                entity.scopeId(),
                entity.sourceRefs(),
                ArchitectureIrDependencyMetadataSupport.immutable(metadata)
            ));
        }
        return Map.copyOf(enriched);
    }
}
