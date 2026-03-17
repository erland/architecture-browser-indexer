package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ArchitectureIrBrowserViewFamilyCatalogSupport {
    private ArchitectureIrBrowserViewFamilyCatalogSupport() {
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
        List<String> availableViews = ArchitectureIrBrowserViewMetadataBuilder.stringList(browserViews.get("availableViews"));
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
}
