package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record JavaWritePathSemantics(
    List<String> writeOperations,
    List<String> writeEntityTypes
) {
    Map<String, Object> methodMetadata(Map<String, Object> base) {
        Map<String, Object> metadata = new LinkedHashMap<>(base == null ? Map.of() : base);
        metadata.put("writePath", true);
        metadata.put("writeOperations", writeOperations == null ? List.of() : List.copyOf(writeOperations));
        metadata.put("writeEntityTypes", writeEntityTypes == null ? List.of() : List.copyOf(writeEntityTypes));
        return Map.copyOf(metadata);
    }
}
