package info.isaksson.erland.architecturebrowser.indexer.scan;

import java.util.List;
import java.util.Set;

public record FileInventory(
    List<FileInventoryEntry> entries,
    int totalFiles,
    int indexedFiles,
    int ignoredFiles,
    Set<String> detectedLanguages,
    Set<String> detectedTechnologyMarkers
) {
}
