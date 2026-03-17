package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record DependencyViewEntry(
    Map<String, Object> identifierFields,
    DependencyViewSummary summary,
    Map<String, Object> metrics,
    Map<String, Object> flags
) {
    static DependencyViewEntry of(
        Map<String, Object> identifierFields,
        DependencyViewSummary summary,
        Map<String, Object> metrics,
        Map<String, Object> flags
    ) {
        return new DependencyViewEntry(
            identifierFields == null ? Map.of() : Map.copyOf(identifierFields),
            summary == null ? DependencyViewSummary.empty() : summary,
            metrics == null ? Map.of() : Map.copyOf(metrics),
            flags == null ? Map.of() : Map.copyOf(flags)
        );
    }

    Map<String, Object> toMetadataMap() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.putAll(identifierFields);
        metadata.putAll(summary.toMetadataMap());
        metadata.putAll(metrics);
        metadata.putAll(flags);
        return ArchitectureIrDependencyMetadataSupport.immutable(metadata);
    }

    record DependencyViewSummary(
        List<String> dependencySources,
        List<String> dependencyCategories,
        List<String> frameworks,
        List<String> frameworkRelationships,
        List<String> architectureViewKinds,
        List<String> evidenceRelationshipIds,
        List<String> evidenceLabels
    ) {
        static DependencyViewSummary empty() {
            return new DependencyViewSummary(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("dependencySources", dependencySources == null ? List.of() : List.copyOf(dependencySources));
            metadata.put("dependencyCategories", dependencyCategories == null ? List.of() : List.copyOf(dependencyCategories));
            metadata.put("frameworks", frameworks == null ? List.of() : List.copyOf(frameworks));
            metadata.put("frameworkRelationships", frameworkRelationships == null ? List.of() : List.copyOf(frameworkRelationships));
            metadata.put("architectureViewKinds", architectureViewKinds == null ? List.of() : List.copyOf(architectureViewKinds));
            metadata.put("evidenceRelationshipIds", evidenceRelationshipIds == null ? List.of() : List.copyOf(evidenceRelationshipIds));
            metadata.put("evidenceLabels", evidenceLabels == null ? List.of() : List.copyOf(evidenceLabels));
            return metadata;
        }
    }
}
