package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.List;

final class ArchitectureIrJavaViewpointBridgeDefinitionCatalog {
    private ArchitectureIrJavaViewpointBridgeDefinitionCatalog() {
    }

    static List<BridgeDefinition> bridgeDefinitions() {
        return List.of(
            new BridgeDefinition(
                "api-surface",
                "API surface",
                "Highlights externally exposed API entrypoints and the first service hop behind them when available.",
                "javaEndpointGraph"
            ),
            new BridgeDefinition(
                "request-handling",
                "Request handling",
                "Highlights request-serving paths from entrypoints through application services.",
                "javaWritePathGraph"
            ),
            new BridgeDefinition(
                "persistence-model",
                "Persistence model",
                "Highlights persistent entities together with persistence access paths.",
                "javaEntityModelGraph"
            ),
            new BridgeDefinition(
                "event-flow",
                "Event flow",
                "Highlights Java publisher, event, and observer relationships prepared for asynchronous flow exploration.",
                "javaEventFlowGraph"
            )
        );
    }

    record BridgeDefinition(
        String canonicalViewpointId,
        String title,
        String description,
        String browserViewId
    ) {
    }
}
