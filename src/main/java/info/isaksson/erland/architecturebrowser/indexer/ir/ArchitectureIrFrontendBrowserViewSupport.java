package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ArchitectureIrFrontendBrowserViewSupport {
    private ArchitectureIrFrontendBrowserViewSupport() {
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
        List<ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition> definitions = List.of(
            new ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition(
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
        for (ArchitectureIrBrowserViewDescriptorFactory.FrontendBrowserViewDefinition definition : definitions) {
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
}
