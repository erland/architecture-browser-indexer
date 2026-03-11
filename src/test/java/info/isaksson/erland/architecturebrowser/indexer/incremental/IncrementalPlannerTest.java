package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalPlannerTest {

    @Test
    void plansReprocessingForAddedAndChangedPaths() {
        IncrementalDiff diff = new IncrementalDiff(
            List.of("src/main/java/NewFile.java"),
            List.of("src/main/java/Changed.java"),
            List.of("src/main/java/Removed.java"),
            List.of("src/main/java/Stable.java")
        );

        IncrementalPlan plan = new IncrementalPlanner().plan(diff);

        assertTrue(plan.incremental());
        assertEquals(List.of("src/main/java/NewFile.java", "src/main/java/Changed.java"), plan.parsePaths());
        assertEquals(List.of("src/main/java/Removed.java"), plan.removedPaths());
        assertEquals("reprocess-added-and-changed", plan.metadata().get("strategy"));
    }

    @Test
    void marksNoopWhenNothingChanged() {
        IncrementalDiff diff = new IncrementalDiff(List.of(), List.of(), List.of(), List.of("src/main/java/A.java"));
        IncrementalPlan plan = new IncrementalPlanner().plan(diff);
        assertTrue(!plan.incremental());
        assertEquals(List.of(), plan.parsePaths());
    }
}
