package info.isaksson.erland.architecturebrowser.indexer.incremental.model;

import java.util.List;
import java.util.Map;

public record IncrementalPlan(
    boolean incremental,
    List<String> parsePaths,
    List<String> removedPaths,
    Map<String, Object> metadata
) {
    public IncrementalPlan {
        parsePaths = parsePaths == null ? List.of() : List.copyOf(parsePaths);
        removedPaths = removedPaths == null ? List.of() : List.copyOf(removedPaths);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
