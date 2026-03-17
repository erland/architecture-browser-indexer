package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record BrowserViewDescriptor(
    String id,
    String title,
    String description,
    String framework,
    String category,
    String typeDependencyViewKey,
    String moduleDependencyViewKey,
    List<String> relationshipTypes,
    boolean available,
    int typeDependencyCount,
    int moduleDependencyCount
) {
    Map<String, Object> toMetadataMap() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", id);
        metadata.put("title", title);
        metadata.put("description", description);
        metadata.put("framework", framework);
        metadata.put("category", category);
        metadata.put("typeDependencyViewKey", typeDependencyViewKey);
        metadata.put("moduleDependencyViewKey", moduleDependencyViewKey);
        metadata.put("relationshipTypes", relationshipTypes == null ? List.of() : List.copyOf(relationshipTypes));
        metadata.put("available", available);
        metadata.put("typeDependencyCount", typeDependencyCount);
        metadata.put("moduleDependencyCount", moduleDependencyCount);
        return Map.copyOf(metadata);
    }
}
