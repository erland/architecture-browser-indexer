package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.List;
import java.util.Map;

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
        ArchitectureIrDependencyViewCatalogSupport.DependencyViewCatalog catalog = ArchitectureIrDependencyViewCatalogSupport.compose(
            entitiesById,
            typeDependencies,
            packageDependencies,
            moduleDependencies,
            evidenceDependencies
        );
        return ArchitectureIrBrowserDependencyViewHandoffSupport.applyBrowserViewHandoff(
            catalog.dependencyViews(),
            catalog.browserViewInputs()
        );
    }
}
