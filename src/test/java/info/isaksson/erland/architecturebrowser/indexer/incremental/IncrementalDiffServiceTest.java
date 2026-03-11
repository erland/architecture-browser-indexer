package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalDiff;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalDiffServiceTest {

    @Test
    void detectsAddedChangedRemovedAndUnchangedPaths() {
        FileFingerprintService fingerprintService = new FileFingerprintService();
        IncrementalSnapshot previous = fingerprintService.createSnapshot(new FileInventory(
            List.of(
                new FileInventoryEntry("src/main/java/A.java", 100, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/B.java", 80, "java", "source", "java", false, List.of())
            ),
            2, 2, 0, Set.of("java"), Set.of()
        ));
        IncrementalSnapshot current = fingerprintService.createSnapshot(new FileInventory(
            List.of(
                new FileInventoryEntry("src/main/java/A.java", 100, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/B.java", 81, "java", "source", "java", false, List.of()),
                new FileInventoryEntry("src/main/java/C.java", 30, "java", "source", "java", false, List.of())
            ),
            3, 3, 0, Set.of("java"), Set.of()
        ));

        IncrementalDiff diff = new IncrementalDiffService().diff(previous, current);

        assertEquals(List.of("src/main/java/C.java"), diff.addedPaths());
        assertEquals(List.of("src/main/java/B.java"), diff.changedPaths());
        assertEquals(List.of(), diff.removedPaths());
        assertEquals(List.of("src/main/java/A.java"), diff.unchangedPaths());
        assertTrue(diff.isIncrementalUseful());
    }

    @Test
    void detectsRemovedPaths() {
        FileFingerprintService fingerprintService = new FileFingerprintService();
        IncrementalSnapshot previous = fingerprintService.createSnapshot(new FileInventory(
            List.of(new FileInventoryEntry("src/main/ts/app.ts", 10, "ts", "source", "typescript", false, List.of())),
            1, 1, 0, Set.of("typescript"), Set.of()
        ));
        IncrementalSnapshot current = fingerprintService.createSnapshot(new FileInventory(
            List.of(),
            0, 0, 0, Set.of(), Set.of()
        ));

        IncrementalDiff diff = new IncrementalDiffService().diff(previous, current);
        assertEquals(List.of("src/main/ts/app.ts"), diff.removedPaths());
    }
}
