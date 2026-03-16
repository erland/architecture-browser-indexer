package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ArchitectureIrDependencyViewPostProcessor {
    private ArchitectureIrDependencyViewPostProcessor() {
    }

    static Map<String, Object> finalizeDependencyViews(
        Map<String, ArchitectureEntity> entitiesById,
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> packageDependencies,
        List<Map<String, Object>> moduleDependencies,
        List<Map<String, Object>> evidenceDependencies
    ) {
        List<Map<String, Object>> packageMetrics = ArchitectureIrPackageMetricsBoundaryBuilder.buildPackageMetrics(entitiesById, packageDependencies);
        List<Map<String, Object>> frameworkTypeDependencies = filterDependenciesByViewKind(typeDependencies, "framework");
        List<Map<String, Object>> frameworkModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "framework");
        List<Map<String, Object>> compositionTypeDependencies = filterDependenciesByViewKind(typeDependencies, "composition");
        List<Map<String, Object>> compositionModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "composition");
        List<Map<String, Object>> routeTypeDependencies = filterDependenciesByViewKind(typeDependencies, "route");
        List<Map<String, Object>> routeModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "route");
        List<Map<String, Object>> providerTypeDependencies = filterDependenciesByViewKind(typeDependencies, "provider-di");
        List<Map<String, Object>> providerModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "provider-di");
        List<Map<String, Object>> hookTypeDependencies = filterDependenciesByViewKind(typeDependencies, "hook");
        List<Map<String, Object>> hookModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "hook");
        List<Map<String, Object>> endpointTypeDependencies = filterDependenciesByViewKind(typeDependencies, "endpoint");
        List<Map<String, Object>> endpointModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "endpoint");
        List<Map<String, Object>> entityModelTypeDependencies = filterDependenciesByViewKind(typeDependencies, "entity-model");
        List<Map<String, Object>> entityModelModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "entity-model");
        List<Map<String, Object>> observerTypeDependencies = filterDependenciesByViewKind(typeDependencies, "observer-event");
        List<Map<String, Object>> observerModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "observer-event");
        List<Map<String, Object>> writePathTypeDependencies = filterDependenciesByViewKind(typeDependencies, "write-path");
        List<Map<String, Object>> writePathModuleDependencies = filterDependenciesByViewKind(moduleDependencies, "write-path");
        Map<String, Object> dependencyViews = new LinkedHashMap<>();
        dependencyViews.put("typeDependencies", List.copyOf(typeDependencies));
        dependencyViews.put("packageDependencies", List.copyOf(packageDependencies));
        dependencyViews.put("moduleDependencies", List.copyOf(moduleDependencies));
        dependencyViews.put("evidenceDependencies", List.copyOf(evidenceDependencies));
        dependencyViews.put("frameworkTypeDependencies", List.copyOf(frameworkTypeDependencies));
        dependencyViews.put("frameworkModuleDependencies", List.copyOf(frameworkModuleDependencies));
        dependencyViews.put("compositionTypeDependencies", List.copyOf(compositionTypeDependencies));
        dependencyViews.put("compositionModuleDependencies", List.copyOf(compositionModuleDependencies));
        dependencyViews.put("routeTypeDependencies", List.copyOf(routeTypeDependencies));
        dependencyViews.put("routeModuleDependencies", List.copyOf(routeModuleDependencies));
        dependencyViews.put("providerTypeDependencies", List.copyOf(providerTypeDependencies));
        dependencyViews.put("providerModuleDependencies", List.copyOf(providerModuleDependencies));
        dependencyViews.put("hookTypeDependencies", List.copyOf(hookTypeDependencies));
        dependencyViews.put("hookModuleDependencies", List.copyOf(hookModuleDependencies));
        dependencyViews.put("endpointTypeDependencies", List.copyOf(endpointTypeDependencies));
        dependencyViews.put("endpointModuleDependencies", List.copyOf(endpointModuleDependencies));
        dependencyViews.put("entityModelTypeDependencies", List.copyOf(entityModelTypeDependencies));
        dependencyViews.put("entityModelModuleDependencies", List.copyOf(entityModelModuleDependencies));
        dependencyViews.put("observerTypeDependencies", List.copyOf(observerTypeDependencies));
        dependencyViews.put("observerModuleDependencies", List.copyOf(observerModuleDependencies));
        dependencyViews.put("writePathTypeDependencies", List.copyOf(writePathTypeDependencies));
        dependencyViews.put("writePathModuleDependencies", List.copyOf(writePathModuleDependencies));
        dependencyViews.put("packageMetrics", List.copyOf(packageMetrics));
        dependencyViews.put("boundarySummary", ArchitectureIrPackageMetricsBoundaryBuilder.buildBoundarySummary(typeDependencies, packageDependencies, moduleDependencies));
        List<String> recommendedEntryPoints = new ArrayList<>(List.of("packageDependencies", "typeDependencies", "moduleDependencies"));
        List<String> primaryArchitectureViews = new ArrayList<>(List.of("packageDependencies", "typeDependencies", "moduleDependencies"));
        if (!frameworkTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("frameworkTypeDependencies");
            primaryArchitectureViews.add("frameworkTypeDependencies");
        }
        if (!frameworkModuleDependencies.isEmpty()) {
            recommendedEntryPoints.add("frameworkModuleDependencies");
            primaryArchitectureViews.add("frameworkModuleDependencies");
        }
        if (!endpointTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("endpointTypeDependencies");
            primaryArchitectureViews.add("endpointTypeDependencies");
        }
        if (!entityModelTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("entityModelTypeDependencies");
            primaryArchitectureViews.add("entityModelTypeDependencies");
        }
        if (!observerTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("observerTypeDependencies");
            primaryArchitectureViews.add("observerTypeDependencies");
        }
        if (!writePathTypeDependencies.isEmpty()) {
            recommendedEntryPoints.add("writePathTypeDependencies");
            primaryArchitectureViews.add("writePathTypeDependencies");
        }
        recommendedEntryPoints.add("evidenceDependencies");
        dependencyViews.put("recommendedEntryPoints", List.copyOf(recommendedEntryPoints));
        dependencyViews.put("primaryArchitectureViews", List.copyOf(primaryArchitectureViews));
        dependencyViews.put("frontendArchitectureViews", Map.of(
            "frameworkAware", List.of("frameworkTypeDependencies", "frameworkModuleDependencies"),
            "composition", List.of("compositionTypeDependencies", "compositionModuleDependencies"),
            "routing", List.of("routeTypeDependencies", "routeModuleDependencies"),
            "providerAndDi", List.of("providerTypeDependencies", "providerModuleDependencies"),
            "hooks", List.of("hookTypeDependencies", "hookModuleDependencies")
        ));
        dependencyViews.put("javaFrameworkArchitectureViews", Map.of(
            "endpoints", List.of("endpointTypeDependencies", "endpointModuleDependencies"),
            "entityModel", List.of("entityModelTypeDependencies", "entityModelModuleDependencies"),
            "observerEvents", List.of("observerTypeDependencies", "observerModuleDependencies"),
            "writePaths", List.of("writePathTypeDependencies", "writePathModuleDependencies")
        ));
        ArchitectureIrBrowserViewComposition browserViewComposition = ArchitectureIrBrowserViewMetadataBuilder.compose(
            new ArchitectureIrBrowserViewCompositionInputs(
                compositionTypeDependencies,
                compositionModuleDependencies,
                routeTypeDependencies,
                routeModuleDependencies,
                providerTypeDependencies,
                providerModuleDependencies,
                hookTypeDependencies,
                hookModuleDependencies,
                endpointTypeDependencies,
                endpointModuleDependencies,
                entityModelTypeDependencies,
                entityModelModuleDependencies,
                observerTypeDependencies,
                observerModuleDependencies,
                writePathTypeDependencies,
                writePathModuleDependencies
            )
        );
        dependencyViews = new LinkedHashMap<>(browserViewComposition.applyTo(dependencyViews));
        dependencyViews.put("evidenceStatus", Map.of(
            "fileImportDependencies", "supporting-evidence",
            "recommendedForArchitectureViews", false,
            "description", "File import dependencies are retained for traceability and drill-down, but higher-level architecture views should prefer package, type, module, and framework-aware dependency rollups."
        ));
        return Map.copyOf(dependencyViews);
    }

    private static List<Map<String, Object>> filterDependenciesByViewKind(List<Map<String, Object>> dependencies, String viewKind) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> dependency : dependencies) {
            if (hasArchitectureViewKind(dependency, viewKind)) {
                filtered.add(dependency);
            }
        }
        return List.copyOf(filtered);
    }

    @SuppressWarnings("unchecked")
    private static boolean hasArchitectureViewKind(Map<String, Object> dependency, String viewKind) {
        if (dependency == null || viewKind == null || viewKind.isBlank()) {
            return false;
        }
        Object value = dependency.get("architectureViewKinds");
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).anyMatch(viewKind::equals);
        }
        return false;
    }
}
