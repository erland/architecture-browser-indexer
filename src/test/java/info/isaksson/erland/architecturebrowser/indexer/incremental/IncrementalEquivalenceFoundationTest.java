package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalPlan;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncrementalEquivalenceFoundationTest {

    @Test
    void incrementalSelectionMatchesChangedSubsetAgainstFullInventory() {
        FileInventory beforeInventory = new FileInventory(
            List.of(
                new FileInventoryEntry("src/main/java/A.java", 100, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/B.java", 100, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/C.java", 100, "java", "source", "java", false, List.of())
            ),
            3, 3, 0, Set.of("java"), Set.of()
        );
        FileInventory afterInventory = new FileInventory(
            List.of(
                new FileInventoryEntry("src/main/java/A.java", 100, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/B.java", 120, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/C.java", 100, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/D.java", 50, "java", "source", "java", false, List.of())
            ),
            4, 4, 0, Set.of("java"), Set.of()
        );

        FileFingerprintService fingerprintService = new FileFingerprintService();
        IncrementalSnapshot before = fingerprintService.createSnapshot(beforeInventory);
        IncrementalSnapshot after = fingerprintService.createSnapshot(afterInventory);

        IncrementalDiff diff = new IncrementalDiffService().diff(before, after);
        IncrementalPlan plan = new IncrementalPlanner().plan(diff);

        assertEquals(List.of("src/main/java/D.java", "src/main/java/B.java"), plan.parsePaths());
        assertEquals(List.of("src/main/java/D.java", "src/main/java/B.java"), diff.reprocessPaths());
    }
}
