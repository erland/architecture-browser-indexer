package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ArchitectureIrBrowserViewDescriptorFactory {
    private ArchitectureIrBrowserViewDescriptorFactory() {
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
            return Collections.unmodifiableMap(metadata);
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
        String relationshipCatalogView,
        List<String> frameworkRelationships,
        List<Map<String, Object>> typeDependencies,
        List<Map<String, Object>> moduleDependencies,
        List<Map<String, Object>> relationshipCatalogEntries
    ) {
        Map<String, Object> toMetadataMap() {
            boolean available = !typeDependencies.isEmpty()
                || !moduleDependencies.isEmpty()
                || (relationshipCatalogEntries != null && !relationshipCatalogEntries.isEmpty());
            Map<String, Object> metadata = new LinkedHashMap<>(new BrowserViewDescriptor(
                id,
                title,
                description,
                framework,
                architectureViewKind,
                typeDependencyView,
                moduleDependencyView,
                frameworkRelationships,
                available,
                typeDependencies.size(),
                moduleDependencies.size()
            ).toMetadataMap());
            metadata.put("architectureViewKind", architectureViewKind);
            metadata.put("typeDependencyView", typeDependencyView);
            metadata.put("moduleDependencyView", moduleDependencyView);
            metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
            if (relationshipCatalogView != null) {
                metadata.put("relationshipCatalogView", relationshipCatalogView);
            }
            String preferredDependencyView = relationshipCatalogView != null && relationshipCatalogEntries != null && !relationshipCatalogEntries.isEmpty()
                ? relationshipCatalogView
                : !typeDependencies.isEmpty()
                ? typeDependencyView
                : !moduleDependencies.isEmpty()
                ? moduleDependencyView
                : relationshipCatalogView;
            metadata.put("preferredDependencyView", preferredDependencyView);
            metadata.put("browserViewKind", relationshipCatalogView != null && relationshipCatalogEntries != null && !relationshipCatalogEntries.isEmpty()
                ? "graph-with-relationship-catalog"
                : "graph");
            metadata.put("recommendedForArchitectureViews", true);
            metadata.put("typeRelationshipKinds", distinctStringValues(typeDependencies, "relationshipKind"));
            metadata.put("moduleRelationshipKinds", distinctStringValues(moduleDependencies, "relationshipKind"));
            metadata.put("availableFrameworks", distinctListValues("frameworks", typeDependencies, moduleDependencies));
            metadata.put("availableArchitectureViewKinds", distinctListValues("architectureViewKinds", typeDependencies, moduleDependencies));
            if (relationshipCatalogEntries != null) {
                metadata.put("relationshipCatalogCount", relationshipCatalogEntries.size());
                metadata.put("relationshipAssociationCardinalities", distinctStringValues(relationshipCatalogEntries, "associationCardinality"));
                metadata.put("relationshipAssociationKinds", distinctStringValues(relationshipCatalogEntries, "associationKind"));
            }
            return Collections.unmodifiableMap(metadata);
        }
    }
}
