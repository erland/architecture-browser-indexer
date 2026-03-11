package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncrementalSnapshotJsonTest {

    @Test
    void writesAndReadsSnapshot() throws Exception {
        IncrementalSnapshot snapshot = new FileFingerprintService().createSnapshot(new FileInventory(
            List.of(new FileInventoryEntry("src/main/java/A.java", 100, "java", "source", "java", false, List.of())),
            1, 1, 0, Set.of("java"), Set.of()
        ));

        Path file = Files.createTempFile("ab-index-snapshot", ".json");
        IncrementalSnapshotJson.write(file, snapshot);
        IncrementalSnapshot loaded = IncrementalSnapshotJson.read(file);

        assertEquals(snapshot.schemaVersion(), loaded.schemaVersion());
        assertEquals(snapshot.filesByPath().keySet(), loaded.filesByPath().keySet());
    }
}
