package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalPlan;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IncrementalPlanner {

    public IncrementalPlan plan(IncrementalDiff diff) {
        boolean incremental = diff != null && diff.isIncrementalUseful();
        if (diff == null) {
            return new IncrementalPlan(false, java.util.List.of(), java.util.List.of(), Map.of("reason", "no-diff"));
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("addedCount", diff.addedPaths().size());
        metadata.put("changedCount", diff.changedPaths().size());
        metadata.put("removedCount", diff.removedPaths().size());
        metadata.put("unchangedCount", diff.unchangedPaths().size());
        metadata.put("strategy", incremental ? "reprocess-added-and-changed" : "full-or-noop");
        return new IncrementalPlan(
            incremental,
            diff.reprocessPaths(),
            diff.removedPaths(),
            Map.copyOf(metadata)
        );
    }
}
