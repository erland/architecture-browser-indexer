package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ArchitectureIrBrowserViewMetadataBuilder {
    private ArchitectureIrBrowserViewMetadataBuilder() {
    }

    static ArchitectureIrBrowserViewComposition compose(ArchitectureIrBrowserViewCompositionInputs inputs) {
        if (inputs == null) {
            return ArchitectureIrBrowserViewComposition.empty();
        }
        Map<String, Object> frontendBrowserViews = ArchitectureIrFrontendBrowserViewSupport.buildFrontendBrowserViews(
            inputs.compositionTypeDependencies(),
            inputs.compositionModuleDependencies(),
            inputs.routeTypeDependencies(),
            inputs.routeModuleDependencies(),
            inputs.providerTypeDependencies(),
            inputs.providerModuleDependencies(),
            inputs.hookTypeDependencies(),
            inputs.hookModuleDependencies()
        );
        Map<String, Object> javaBrowserViews = ArchitectureIrJavaBrowserViewSupport.buildJavaBrowserViews(
            inputs.endpointTypeDependencies(),
            inputs.endpointModuleDependencies(),
            inputs.entityModelTypeDependencies(),
            inputs.entityModelModuleDependencies(),
            inputs.observerTypeDependencies(),
            inputs.observerModuleDependencies(),
            inputs.writePathTypeDependencies(),
            inputs.writePathModuleDependencies()
        );
        Map<String, Object> browserViewCatalog = ArchitectureIrBrowserViewFamilyCatalogSupport.buildBrowserViewCatalog(frontendBrowserViews, javaBrowserViews);
        return new ArchitectureIrBrowserViewComposition(frontendBrowserViews, javaBrowserViews, browserViewCatalog);
    }

    static Map<String, Object> buildFrontendBrowserViews(
        List<Map<String, Object>> compositionTypeDependencies,
        List<Map<String, Object>> compositionModuleDependencies,
        List<Map<String, Object>> routeTypeDependencies,
        List<Map<String, Object>> routeModuleDependencies,
        List<Map<String, Object>> providerTypeDependencies,
        List<Map<String, Object>> providerModuleDependencies,
        List<Map<String, Object>> hookTypeDependencies,
        List<Map<String, Object>> hookModuleDependencies
    ) {
        return ArchitectureIrFrontendBrowserViewSupport.buildFrontendBrowserViews(
            compositionTypeDependencies,
            compositionModuleDependencies,
            routeTypeDependencies,
            routeModuleDependencies,
            providerTypeDependencies,
            providerModuleDependencies,
            hookTypeDependencies,
            hookModuleDependencies
        );
    }

    static Map<String, Object> buildJavaBrowserViews(
        List<Map<String, Object>> endpointTypeDependencies,
        List<Map<String, Object>> endpointModuleDependencies,
        List<Map<String, Object>> entityModelTypeDependencies,
        List<Map<String, Object>> entityModelModuleDependencies,
        List<Map<String, Object>> observerTypeDependencies,
        List<Map<String, Object>> observerModuleDependencies,
        List<Map<String, Object>> writePathTypeDependencies,
        List<Map<String, Object>> writePathModuleDependencies
    ) {
        return ArchitectureIrJavaBrowserViewSupport.buildJavaBrowserViews(
            endpointTypeDependencies,
            endpointModuleDependencies,
            entityModelTypeDependencies,
            entityModelModuleDependencies,
            observerTypeDependencies,
            observerModuleDependencies,
            writePathTypeDependencies,
            writePathModuleDependencies
        );
    }

    static Map<String, Object> buildBrowserViewCatalog(
        Map<String, Object> frontendBrowserViews,
        Map<String, Object> javaBrowserViews
    ) {
        return ArchitectureIrBrowserViewFamilyCatalogSupport.buildBrowserViewCatalog(frontendBrowserViews, javaBrowserViews);
    }

    @SuppressWarnings("unchecked")
    static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of();
    }

    static List<String> distinctStringValues(List<Map<String, Object>> dependencies, String key) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (Map<String, Object> dependency : dependencies) {
            Object value = dependency.get(key);
            if (value != null) {
                values.add(String.valueOf(value));
            }
        }
        return List.copyOf(values);
    }

    @SafeVarargs
    static List<String> distinctListValues(String key, List<Map<String, Object>>... dependencyGroups) {
        Set<String> values = new LinkedHashSet<>();
        if (dependencyGroups != null) {
            for (List<Map<String, Object>> group : dependencyGroups) {
                if (group == null) {
                    continue;
                }
                for (Map<String, Object> dependency : group) {
                    values.addAll(stringList(dependency.get(key)));
                }
            }
        }
        return List.copyOf(values);
    }

    static List<Map<String, Object>> filterDependenciesForFrontendBrowserView(
        List<Map<String, Object>> dependencies,
        String framework,
        List<String> frameworkRelationships
    ) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }
        return dependencies.stream()
            .filter(dependency -> {
                List<String> frameworks = stringList(dependency.get("frameworks"));
                if (!"frontend".equals(framework) && !frameworks.contains(framework)) {
                    return false;
                }
                if (frameworkRelationships == null || frameworkRelationships.isEmpty()) {
                    return true;
                }
                List<String> relationships = stringList(dependency.get("frameworkRelationships"));
                return relationships.stream().anyMatch(frameworkRelationships::contains);
            })
            .toList();
    }

    record FrontendBrowserViewDefinition(
        String id,
        String title,
        String description,
        String framework,
        String architectureViewKind,
        String typeDependencyView,
        String moduleDependencyView,
        List<String> frameworkRelationships,
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> moduleDependencies
    ) {
        Map<String, Object> toMetadataMap() {
            List<Map<String, Object>> filteredTypeDependencies = filterDependenciesForFrontendBrowserView(typeDependencies, framework, frameworkRelationships);
            List<Map<String, Object>> filteredModuleDependencies = filterDependenciesForFrontendBrowserView(moduleDependencies, framework, frameworkRelationships);
            Map<String, Object> metadata = new LinkedHashMap<>(new BrowserViewDescriptor(
                id,
                title,
                description,
                framework,
                architectureViewKind,
                typeDependencyView,
                moduleDependencyView,
                frameworkRelationships,
                !filteredTypeDependencies.isEmpty() || !filteredModuleDependencies.isEmpty(),
                filteredTypeDependencies.size(),
                filteredModuleDependencies.size()
            ).toMetadataMap());
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("preferredDependencyView", !filteredTypeDependencies.isEmpty() ? typeDependencyView : moduleDependencyView);
            metadata.put("browserViewKind", "graph");
            metadata.put("recommendedForArchitectureViews", true);
            return Map.copyOf(metadata);
        }
    }

    record JavaBrowserViewDefinition(
        String id,
        String title,
        String description,
        String framework,
        String architectureViewKind,
        String typeDependencyView,
        String moduleDependencyView,
        List<String> frameworkRelationships,
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> moduleDependencies
    ) {
        Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>(new BrowserViewDescriptor(
                id,
                title,
                description,
                framework,
                architectureViewKind,
                typeDependencyView,
                moduleDependencyView,
                frameworkRelationships,
                !typeDependencies.isEmpty() || !moduleDependencies.isEmpty(),
                typeDependencies.size(),
                moduleDependencies.size()
            ).toMetadataMap());
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("preferredDependencyView", !typeDependencies.isEmpty() ? typeDependencyView : moduleDependencyView);
            metadata.put("browserViewKind", "graph");
            metadata.put("recommendedForArchitectureViews", true);
            metadata.put("typeRelationshipKinds", distinctStringValues(typeDependencies, "relationshipKind"));
            metadata.put("moduleRelationshipKinds", distinctStringValues(moduleDependencies, "relationshipKind"));
            metadata.put("availableFrameworks", distinctListValues("frameworks", typeDependencies, moduleDependencies));
            metadata.put("availableArchitectureViewKinds", distinctListValues("architectureViewKinds", typeDependencies, moduleDependencies));
            return Map.copyOf(metadata);
        }
    }
}
