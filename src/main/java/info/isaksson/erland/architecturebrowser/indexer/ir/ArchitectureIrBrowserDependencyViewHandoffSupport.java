package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.LinkedHashMap;
import java.util.Map;

final class ArchitectureIrBrowserDependencyViewHandoffSupport {
    private ArchitectureIrBrowserDependencyViewHandoffSupport() {
    }

    static Map<String, Object> applyBrowserViewHandoff(
        Map<String, Object> dependencyViews,
        ArchitectureIrBrowserViewCompositionInputs browserViewInputs
    ) {
        ArchitectureIrBrowserViewComposition browserViewComposition = ArchitectureIrBrowserViewMetadataBuilder.compose(browserViewInputs);
        Map<String, Object> result = new LinkedHashMap<>(browserViewComposition.applyTo(dependencyViews));
        if (!browserViewComposition.frontendBrowserViews().isEmpty()) {
            result.put("hasFrontendBrowserView", Boolean.TRUE);
        }
        if (!browserViewComposition.javaBrowserViews().isEmpty()) {
            result.put("hasJavaBrowserView", Boolean.TRUE);
        }
        result.put("evidenceStatus", Map.of(
            "fileImportDependencies", "supporting-evidence",
            "recommendedForArchitectureViews", false,
            "description", "File import dependencies are retained for traceability and drill-down, but higher-level architecture views should prefer package, type, module, and framework-aware dependency rollups."
        ));
        return Map.copyOf(result);
    }
}
