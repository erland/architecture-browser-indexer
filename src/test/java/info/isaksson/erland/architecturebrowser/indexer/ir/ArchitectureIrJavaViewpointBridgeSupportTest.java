package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrJavaViewpointBridgeSupportTest {

    @Test
    void enrichesExistingCanonicalViewpointsAndMaterializesEventFlowFromJavaBrowserViews() {
        List<ArchitectureViewpoint> bridged = ArchitectureIrJavaViewpointBridgeSupport.apply(
            List.of(
                new ArchitectureViewpoint(
                    "api-surface",
                    "API surface",
                    "Highlights externally exposed API entrypoints and the first service hop behind them when available.",
                    "available",
                    0.92,
                    List.of("entity:resource"),
                    List.of("api-entrypoint"),
                    List.of("serves-request"),
                    null,
                    List.of("normalized-roles")
                ),
                new ArchitectureViewpoint(
                    "request-handling",
                    "Request handling",
                    "Highlights request-serving paths from entrypoints through application services.",
                    "available",
                    0.9,
                    List.of("entity:resource", "entity:service"),
                    List.of("api-entrypoint", "application-service"),
                    List.of("invokes-use-case", "serves-request"),
                    null,
                    List.of("normalized-semantics")
                ),
                new ArchitectureViewpoint(
                    "persistence-model",
                    "Persistence model",
                    "Highlights persistent entities together with persistence access paths.",
                    "available",
                    0.88,
                    List.of("entity:repo", "entity:order"),
                    List.of("persistent-entity", "persistence-access"),
                    List.of("accesses-persistence"),
                    null,
                    List.of("normalized-roles")
                )
            ),
            dependencyViews()
        );

        ArchitectureViewpoint apiSurface = viewpointById(bridged, "api-surface");
        assertEquals(List.of("endpointModuleDependencies", "endpointTypeDependencies"), apiSurface.preferredDependencyViews());
        assertTrue(apiSurface.evidenceSources().contains("java-browser-views"));
        assertTrue(apiSurface.evidenceSources().contains("java-dependency-views"));

        ArchitectureViewpoint requestHandling = viewpointById(bridged, "request-handling");
        assertEquals(List.of("writePathModuleDependencies", "writePathTypeDependencies"), requestHandling.preferredDependencyViews());

        ArchitectureViewpoint persistenceModel = viewpointById(bridged, "persistence-model");
        assertEquals(List.of("entityModelModuleDependencies", "entityModelTypeDependencies"), persistenceModel.preferredDependencyViews());

        ArchitectureViewpoint eventFlow = viewpointById(bridged, "event-flow");
        assertEquals("available", eventFlow.availability());
        assertEquals(List.of("observerModuleDependencies", "observerTypeDependencies"), eventFlow.preferredDependencyViews());
        assertTrue(eventFlow.evidenceSources().contains("java-browser-views"));
    }

    private static Map<String, Object> dependencyViews() {
        return Map.of(
            "endpointTypeDependencies", List.of(Map.of("from", "entity:resource", "to", "entity:service")),
            "endpointModuleDependencies", List.of(Map.of("from", "module:api", "to", "module:service")),
            "entityModelTypeDependencies", List.of(Map.of("from", "entity:order", "to", "entity:line")),
            "entityModelModuleDependencies", List.of(),
            "observerTypeDependencies", List.of(Map.of("from", "entity:publisher", "to", "entity:event")),
            "observerModuleDependencies", List.of(Map.of("from", "module:events", "to", "module:listeners")),
            "writePathTypeDependencies", List.of(Map.of("from", "entity:service", "to", "entity:repo")),
            "writePathModuleDependencies", List.of(Map.of("from", "module:service", "to", "module:persistence")),
            "javaBrowserViews", Map.of(
                "views", List.of(
                    Map.of(
                        "id", "javaEndpointGraph",
                        "available", true,
                        "preferredDependencyView", "endpointTypeDependencies",
                        "typeDependencyView", "endpointTypeDependencies",
                        "moduleDependencyView", "endpointModuleDependencies",
                        "typeDependencyCount", 1,
                        "moduleDependencyCount", 1
                    ),
                    Map.of(
                        "id", "javaEntityModelGraph",
                        "available", true,
                        "preferredDependencyView", "entityModelTypeDependencies",
                        "typeDependencyView", "entityModelTypeDependencies",
                        "moduleDependencyView", "entityModelModuleDependencies",
                        "typeDependencyCount", 1,
                        "moduleDependencyCount", 0
                    ),
                    Map.of(
                        "id", "javaEventFlowGraph",
                        "available", true,
                        "preferredDependencyView", "observerTypeDependencies",
                        "typeDependencyView", "observerTypeDependencies",
                        "moduleDependencyView", "observerModuleDependencies",
                        "typeDependencyCount", 1,
                        "moduleDependencyCount", 1
                    ),
                    Map.of(
                        "id", "javaWritePathGraph",
                        "available", true,
                        "preferredDependencyView", "writePathTypeDependencies",
                        "typeDependencyView", "writePathTypeDependencies",
                        "moduleDependencyView", "writePathModuleDependencies",
                        "typeDependencyCount", 1,
                        "moduleDependencyCount", 1
                    )
                )
            )
        );
    }

    private static ArchitectureViewpoint viewpointById(List<ArchitectureViewpoint> viewpoints, String id) {
        return viewpoints.stream()
            .filter(viewpoint -> id.equals(viewpoint.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing viewpoint=" + id + " viewpoints=" + viewpoints));
    }
}
