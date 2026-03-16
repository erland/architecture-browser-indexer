package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.LinkedHashMap;
import java.util.Map;

record ArchitectureIrBrowserViewComposition(
    Map<String, Object> frontendBrowserViews,
    Map<String, Object> javaBrowserViews,
    Map<String, Object> browserViewCatalog
) {
    static ArchitectureIrBrowserViewComposition empty() {
        return new ArchitectureIrBrowserViewComposition(Map.of(), Map.of(), Map.of());
    }

    Map<String, Object> applyTo(Map<String, Object> dependencyViews) {
        Map<String, Object> result = new LinkedHashMap<>(dependencyViews);
        if (!frontendBrowserViews.isEmpty()) {
            result.put("frontendBrowserViews", frontendBrowserViews);
        }
        if (!javaBrowserViews.isEmpty()) {
            result.put("javaBrowserViews", javaBrowserViews);
        }
        if (!browserViewCatalog.isEmpty()) {
            result.put("browserViewCatalog", browserViewCatalog);
        }
        return Map.copyOf(result);
    }
}
