package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ArchitectureIrJavaBrowserViewSupport {
    private ArchitectureIrJavaBrowserViewSupport() {
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
        List<ArchitectureIrBrowserViewMetadataBuilder.JavaBrowserViewDefinition> definitions = List.of(
            new ArchitectureIrBrowserViewMetadataBuilder.JavaBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewMetadataBuilder.JavaBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewMetadataBuilder.JavaBrowserViewDefinition(
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
            new ArchitectureIrBrowserViewMetadataBuilder.JavaBrowserViewDefinition(
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
        for (ArchitectureIrBrowserViewMetadataBuilder.JavaBrowserViewDefinition definition : definitions) {
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
}
