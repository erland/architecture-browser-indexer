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
        List<FrontendBrowserViewDefinition> definitions = List.of(
            new FrontendBrowserViewDefinition(
                "angularModuleGraph",
                "Angular module graph",
                "Angular module, standalone component, and template composition relationships for browser-native graph exploration.",
                "angular",
                "composition",
                "compositionTypeDependencies",
                "compositionModuleDependencies",
                List.of("declares", "imports", "exports", "bootstraps", "templateRenders", "usesDirective", "usesPipe"),
                compositionTypeDependencies,
                compositionModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "angularProviderGraph",
                "Angular provider graph",
                "Angular provider, injection-token, and dependency-injection relationships for browser-native graph exploration.",
                "angular",
                "provider-di",
                "providerTypeDependencies",
                "providerModuleDependencies",
                List.of("provides", "providedBy", "injects", "resolvesTo"),
                providerTypeDependencies,
                providerModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "routeGraph",
                "Frontend route graph",
                "Angular and React routing relationships for browser-native navigation and path analysis.",
                "frontend",
                "route",
                "routeTypeDependencies",
                "routeModuleDependencies",
                List.of("targets", "childOf", "lazyLoads", "guards", "resolves"),
                routeTypeDependencies,
                routeModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "reactComponentCompositionGraph",
                "React component composition graph",
                "React render/composition relationships for browser-native component graph exploration.",
                "react",
                "composition",
                "compositionTypeDependencies",
                "compositionModuleDependencies",
                List.of("renders"),
                compositionTypeDependencies,
                compositionModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "reactContextGraph",
                "React context graph",
                "React provider/consumer context relationships for browser-native context exploration.",
                "react",
                "provider-di",
                "providerTypeDependencies",
                "providerModuleDependencies",
                List.of("providesContext", "consumesContext"),
                providerTypeDependencies,
                providerModuleDependencies
            ),
            new FrontendBrowserViewDefinition(
                "reactHookGraph",
                "React hook graph",
                "React custom-hook usage relationships for browser-native hook exploration.",
                "react",
                "hook",
                "hookTypeDependencies",
                "hookModuleDependencies",
                List.of("usesHook"),
                hookTypeDependencies,
                hookModuleDependencies
            )
        );

        List<Map<String, Object>> views = new ArrayList<>();
        List<String> availableViews = new ArrayList<>();
        for (FrontendBrowserViewDefinition definition : definitions) {
            Map<String, Object> descriptor = definition.toMetadataMap();
            views.add(descriptor);
            if (Boolean.TRUE.equals(descriptor.get("available"))) {
                availableViews.add(definition.id());
            }
        }
        if (availableViews.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("views", List.copyOf(views));
        result.put("availableViews", List.copyOf(availableViews));
        result.put("defaultViewId", availableViews.get(0));
        result.put("description", "Browser-facing frontend graph descriptors derived from existing dependency rollups.");
        return Map.copyOf(result);
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
        List<JavaBrowserViewDefinition> definitions = List.of(
            new JavaBrowserViewDefinition(
                "javaEndpointGraph",
                "Java endpoint graph",
                "JAX-RS resource and endpoint relationships prepared for browser-native backend API exploration.",
                "jax-rs",
                "endpoint",
                "endpointTypeDependencies",
                "endpointModuleDependencies",
                List.of("exposesEndpoint", "endpoint"),
                endpointTypeDependencies,
                endpointModuleDependencies
            ),
            new JavaBrowserViewDefinition(
                "javaEntityModelGraph",
                "Java entity model graph",
                "JPA entity, embeddable, inheritance, and association relationships prepared for browser-native persistence-model exploration.",
                "jpa",
                "entity-model",
                "entityModelTypeDependencies",
                "entityModelModuleDependencies",
                List.of("hasAssociation", "embeds", "inheritsPersistenceModel"),
                entityModelTypeDependencies,
                entityModelModuleDependencies
            ),
            new JavaBrowserViewDefinition(
                "javaEventFlowGraph",
                "Java CDI event flow graph",
                "CDI publisher, event, and observer relationships prepared for browser-native asynchronous flow exploration.",
                "cdi",
                "observer-event",
                "observerTypeDependencies",
                "observerModuleDependencies",
                List.of("publishesEvent", "observesEvent", "eventObservedBy"),
                observerTypeDependencies,
                observerModuleDependencies
            ),
            new JavaBrowserViewDefinition(
                "javaWritePathGraph",
                "Java write path graph",
                "Service and repository write-path relationships prepared for browser-native persistence flow exploration.",
                "jpa",
                "write-path",
                "writePathTypeDependencies",
                "writePathModuleDependencies",
                List.of("writePath"),
                writePathTypeDependencies,
                writePathModuleDependencies
            )
        );

        List<Map<String, Object>> views = new ArrayList<>();
        List<String> availableViews = new ArrayList<>();
        for (JavaBrowserViewDefinition definition : definitions) {
            Map<String, Object> descriptor = definition.toMetadataMap();
            views.add(descriptor);
            if (Boolean.TRUE.equals(descriptor.get("available"))) {
                availableViews.add(definition.id());
            }
        }
        if (availableViews.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("views", List.copyOf(views));
        result.put("availableViews", List.copyOf(availableViews));
        result.put("defaultViewId", availableViews.get(0));
        result.put("description", "Browser-facing Java backend graph descriptors derived from framework-aware dependency rollups.");
        result.put("recommendedEntryPoints", List.of(
            "javaEndpointGraph",
            "javaEntityModelGraph",
            "javaEventFlowGraph",
            "javaWritePathGraph"
        ));
        return Map.copyOf(result);
    }

    static Map<String, Object> buildBrowserViewCatalog(
        Map<String, Object> frontendBrowserViews,
        Map<String, Object> javaBrowserViews
    ) {
        List<Map<String, Object>> groups = new ArrayList<>();
        List<String> availableFamilies = new ArrayList<>();
        addBrowserViewFamily(groups, availableFamilies, "frontend", "Frontend browser views", frontendBrowserViews);
        addBrowserViewFamily(groups, availableFamilies, "java", "Java backend browser views", javaBrowserViews);
        if (availableFamilies.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("families", List.copyOf(groups));
        result.put("availableFamilies", List.copyOf(availableFamilies));
        result.put("defaultFamily", availableFamilies.get(0));
        result.put("description", "High-level browser view families exported with the architecture index document.");
        return Map.copyOf(result);
    }

    private static void addBrowserViewFamily(
        List<Map<String, Object>> groups,
        List<String> availableFamilies,
        String id,
        String title,
        Map<String, Object> browserViews
    ) {
        if (browserViews == null || browserViews.isEmpty()) {
            return;
        }
        List<String> availableViews = stringList(browserViews.get("availableViews"));
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("id", id);
        group.put("title", title);
        group.put("available", !availableViews.isEmpty());
        group.put("availableViewIds", availableViews);
        group.put("defaultViewId", browserViews.get("defaultViewId"));
        group.put("viewCount", ((List<?>) browserViews.getOrDefault("views", List.of())).size());
        group.put("description", browserViews.get("description"));
        groups.add(Map.copyOf(group));
        if (!availableViews.isEmpty()) {
            availableFamilies.add(id);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of();
    }

    private static List<String> distinctStringValues(List<Map<String, Object>> dependencies, String key) {
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
    private static List<String> distinctListValues(String key, List<Map<String, Object>>... dependencyGroups) {
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

    private static List<Map<String, Object>> filterDependenciesForFrontendBrowserView(
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

    private record FrontendBrowserViewDefinition(
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
        private Map<String, Object> toMetadataMap() {
            List<Map<String, Object>> filteredTypeDependencies = filterDependenciesForFrontendBrowserView(typeDependencies, framework, frameworkRelationships);
            List<Map<String, Object>> filteredModuleDependencies = filterDependenciesForFrontendBrowserView(moduleDependencies, framework, frameworkRelationships);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", id);
            metadata.put("title", title);
            metadata.put("description", description);
            metadata.put("framework", framework);
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("available", !filteredTypeDependencies.isEmpty() || !filteredModuleDependencies.isEmpty());
            metadata.put("typeDependencyCount", filteredTypeDependencies.size());
            metadata.put("moduleDependencyCount", filteredModuleDependencies.size());
            metadata.put("preferredDependencyView", !filteredTypeDependencies.isEmpty() ? typeDependencyView : moduleDependencyView);
            metadata.put("browserViewKind", "graph");
            metadata.put("recommendedForArchitectureViews", true);
            return Map.copyOf(metadata);
        }
    }

    private record JavaBrowserViewDefinition(
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
        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", id);
            metadata.put("title", title);
            metadata.put("description", description);
            metadata.put("framework", framework);
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            metadata.put("available", !typeDependencies.isEmpty() || !moduleDependencies.isEmpty());
            metadata.put("typeDependencyCount", typeDependencies.size());
            metadata.put("moduleDependencyCount", moduleDependencies.size());
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
