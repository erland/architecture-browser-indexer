package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges existing Java browser/dependency view metadata into the canonical
 * viewpoint catalog so downstream consumers can prefer canonical ids without losing
 * migration-safe access to the old Java-specific graph descriptors.
 */
final class ArchitectureIrJavaViewpointBridgeSupport {
    private ArchitectureIrJavaViewpointBridgeSupport() {
    }

    static List<ArchitectureViewpoint> apply(
        List<ArchitectureViewpoint> viewpoints,
        Map<String, Object> dependencyViews
    ) {
        List<ArchitectureViewpoint> baseViewpoints = viewpoints == null ? List.of() : List.copyOf(viewpoints);
        Map<String, Object> safeDependencyViews = dependencyViews == null ? Map.of() : Map.copyOf(dependencyViews);
        Map<String, Map<String, Object>> javaViewsById = ArchitectureIrJavaViewpointBridgePolicy.javaBrowserViewsById(safeDependencyViews);
        if (javaViewsById.isEmpty()) {
            return baseViewpoints;
        }

        Map<String, ArchitectureViewpoint> byId = new LinkedHashMap<>();
        for (ArchitectureViewpoint viewpoint : baseViewpoints) {
            if (viewpoint != null && viewpoint.id() != null) {
                byId.put(viewpoint.id(), viewpoint);
            }
        }

        for (ArchitectureIrJavaViewpointBridgeDefinitionCatalog.BridgeDefinition definition : ArchitectureIrJavaViewpointBridgeDefinitionCatalog.bridgeDefinitions()) {
            Map<String, Object> javaView = javaViewsById.get(definition.browserViewId());
            if (javaView == null) {
                continue;
            }
            ArchitectureViewpoint existing = byId.get(definition.canonicalViewpointId());
            if (existing != null) {
                byId.put(definition.canonicalViewpointId(), ArchitectureIrJavaViewpointBridgePolicy.mergeExisting(existing, javaView, safeDependencyViews));
            } else if (ArchitectureIrJavaViewpointBridgePolicy.shouldMaterializeBridgedViewpoint(javaView, safeDependencyViews)) {
                byId.put(definition.canonicalViewpointId(), ArchitectureIrJavaViewpointBridgePolicy.createBridgedViewpoint(definition, javaView, safeDependencyViews));
            }
        }
        return List.copyOf(byId.values());
    }
}
