package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record JavaObserverSemantics(
    String observedEventType,
    boolean async,
    List<String> qualifiers
) {
    Map<String, Object> methodMetadata(Map<String, Object> base) {
        Map<String, Object> metadata = new LinkedHashMap<>(base == null ? Map.of() : base);
        metadata.put("framework", "cdi");
        metadata.put("cdiObserver", true);
        metadata.put("cdiObservedEventType", observedEventType);
        metadata.put("observerAsync", async);
        metadata.put("observerQualifiers", qualifiers == null ? List.of() : List.copyOf(qualifiers));
        return Map.copyOf(metadata);
    }
}
