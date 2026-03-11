package info.isaksson.erland.architecturebrowser.indexer.incremental.model;

import java.util.List;

public record IncrementalDiff(
    List<String> addedPaths,
    List<String> changedPaths,
    List<String> removedPaths,
    List<String> unchangedPaths
) {
    public IncrementalDiff {
        addedPaths = addedPaths == null ? List.of() : List.copyOf(addedPaths);
        changedPaths = changedPaths == null ? List.of() : List.copyOf(changedPaths);
        removedPaths = removedPaths == null ? List.of() : List.copyOf(removedPaths);
        unchangedPaths = unchangedPaths == null ? List.of() : List.copyOf(unchangedPaths);
    }

    public boolean isIncrementalUseful() {
        return !addedPaths.isEmpty() || !changedPaths.isEmpty() || !removedPaths.isEmpty();
    }

    public List<String> reprocessPaths() {
        java.util.ArrayList<String> paths = new java.util.ArrayList<>();
        paths.addAll(addedPaths);
        paths.addAll(changedPaths);
        return java.util.List.copyOf(paths);
    }
}
