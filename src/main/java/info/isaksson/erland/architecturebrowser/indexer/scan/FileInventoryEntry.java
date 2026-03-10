package info.isaksson.erland.architecturebrowser.indexer.scan;

import java.util.List;

public record FileInventoryEntry(
    String relativePath,
    long size,
    String extension,
    String type,
    String detectedLanguage,
    boolean ignored,
    List<String> candidateTechnologyMarkers
) {
}
