package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ArchitectureIrJavaViewpointBridgePolicy {
    private ArchitectureIrJavaViewpointBridgePolicy() {
    }

    static ArchitectureViewpoint mergeExisting(
        ArchitectureViewpoint viewpoint,
        Map<String, Object> javaView,
        Map<String, Object> dependencyViews
    ) {
        return new ArchitectureViewpoint(
            viewpoint.id(),
            viewpoint.title(),
            viewpoint.description(),
            viewpoint.availability(),
            viewpoint.confidence(),
            viewpoint.seedEntityIds(),
            viewpoint.seedRoleIds(),
            viewpoint.expandViaSemantics(),
            mergeStrings(viewpoint.preferredDependencyViews(), preferredDependencyViews(javaView, dependencyViews)),
            mergeStrings(viewpoint.evidenceSources(), bridgeEvidenceSources(javaView, dependencyViews))
        );
    }

    static ArchitectureViewpoint createBridgedViewpoint(
        ArchitectureIrJavaViewpointBridgeDefinitionCatalog.BridgeDefinition definition,
        Map<String, Object> javaView,
        Map<String, Object> dependencyViews
    ) {
        boolean available = Boolean.TRUE.equals(javaView.get("available"));
        return new ArchitectureViewpoint(
            definition.canonicalViewpointId(),
            definition.title(),
            definition.description(),
            available ? "available" : "partial",
            confidence(javaView),
            null,
            null,
            null,
            preferredDependencyViews(javaView, dependencyViews),
            bridgeEvidenceSources(javaView, dependencyViews)
        );
    }

    static boolean shouldMaterializeBridgedViewpoint(
        Map<String, Object> javaView,
        Map<String, Object> dependencyViews
    ) {
        return Boolean.TRUE.equals(javaView.get("available"))
            || !preferredDependencyViews(javaView, dependencyViews).isEmpty();
    }

    static List<String> preferredDependencyViews(
        Map<String, Object> javaView,
        Map<String, Object> dependencyViews
    ) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        addDependencyViewId(ids, javaView.get("preferredDependencyView"), dependencyViews);
        addDependencyViewId(ids, javaView.get("typeDependencyView"), dependencyViews);
        addDependencyViewId(ids, javaView.get("moduleDependencyView"), dependencyViews);
        addDependencyViewId(ids, javaView.get("relationshipCatalogView"), dependencyViews);
        return ids.stream().sorted().toList();
    }

    static List<String> bridgeEvidenceSources(
        Map<String, Object> javaView,
        Map<String, Object> dependencyViews
    ) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        sources.add("java-browser-views");
        if (!preferredDependencyViews(javaView, dependencyViews).isEmpty()) {
            sources.add("java-dependency-views");
        }
        return List.copyOf(sources);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> javaBrowserViewsById(Map<String, Object> dependencyViews) {
        Object javaBrowserViews = dependencyViews.get("javaBrowserViews");
        if (!(javaBrowserViews instanceof Map<?, ?> browserViewsMap)) {
            return Map.of();
        }
        Object views = browserViewsMap.get("views");
        if (!(views instanceof List<?> viewList)) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Object candidate : viewList) {
            if (candidate instanceof Map<?, ?> rawView) {
                Object id = rawView.get("id");
                if (id != null) {
                    result.put(String.valueOf(id), (Map<String, Object>) rawView);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static void addDependencyViewId(
        LinkedHashSet<String> ids,
        Object candidate,
        Map<String, Object> dependencyViews
    ) {
        if (candidate == null) {
            return;
        }
        String id = String.valueOf(candidate).trim();
        if (id.isEmpty()) {
            return;
        }
        if (dependencyViews.containsKey(id)) {
            ids.add(id);
        }
    }

    private static List<String> mergeStrings(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return merged.isEmpty() ? null : merged.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty()).sorted().toList();
    }

    private static double confidence(Map<String, Object> javaView) {
        int typeCount = intValue(javaView.get("typeDependencyCount"));
        int moduleCount = intValue(javaView.get("moduleDependencyCount"));
        if (Boolean.TRUE.equals(javaView.get("available"))) {
            return clamp(0.68 + Math.min(0.20, typeCount * 0.05) + Math.min(0.12, moduleCount * 0.04));
        }
        return clamp(0.36 + Math.min(0.10, typeCount * 0.03) + Math.min(0.08, moduleCount * 0.02));
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
